package com.largeevent.management.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.model.EventInfo;
import com.largeevent.management.model.EventSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InitializationRepository {

    private final LocalDatabaseHelper databaseHelper;
    private List<CertificateInfo> cachedCertificates = new ArrayList<>();
    private BasicInfo cachedBasicInfo;
    private EventInfo cachedEventInfo;
    private String cachedRawPayload;
    private List<BasicInfo.ActiveModel> cachedActiveList;

    public InitializationRepository(Context context) {
        databaseHelper = new LocalDatabaseHelper(context.getApplicationContext());
    }

    public synchronized long saveBaseInfo(String serverUrl, String baseInfoJson) throws JSONException {
        JSONObject payload = loadCurrentPayload();
        payload.put("baseInfo", new JSONObject(baseInfoJson));
        if (!payload.has("activeUsers")) {
            payload.put("activeUsers", new JSONArray());
        }
        return insertPayload(serverUrl, payload.toString());
    }

    /** 保存 getBasicInfo 的 data（{@link com.largeevent.management.network.dto.BasicInfoDTO}） */
    public synchronized long saveBaseInfo(String serverUrl,
                                          com.largeevent.management.network.dto.BasicInfoDTO dto)
            throws JSONException {
        String json = com.largeevent.management.network.NetworkManager.getInstance()
                .getGson().toJson(dto != null ? dto : new com.largeevent.management.network.dto.BasicInfoDTO());
        return saveBaseInfo(serverUrl, json);
    }

    public synchronized long saveActiveList(String serverUrl, String activeListJson) throws JSONException {
        JSONObject payload = loadCurrentPayload();
        payload.put("activeList", new JSONArray(activeListJson));
        if (!payload.has("baseInfo")) {
            payload.put("baseInfo", new JSONObject());
        }
        if (!payload.has("activeUsers")) {
            payload.put("activeUsers", new JSONArray());
        }
        return insertPayload(serverUrl, payload.toString());
    }

    public synchronized long saveActiveUsers(String serverUrl, String activeUsersJson) throws JSONException {
        JSONObject payload = loadCurrentPayload();
        payload.put("activeUsers", new JSONArray(activeUsersJson));
        if (!payload.has("baseInfo")) {
            payload.put("baseInfo", new JSONObject());
        }
        return insertPayload(serverUrl, payload.toString());
    }

    private long insertPayload(String serverUrl, String payload) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("server_url", serverUrl);
        values.put("payload", payload);
        values.put("created_at", System.currentTimeMillis());
        return database.insert(LocalDatabaseHelper.TABLE_INIT_DATA, null, values);
    }

    public String getLatestPayload() {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor = database.query(
                LocalDatabaseHelper.TABLE_INIT_DATA,
                new String[]{"payload"},
                null,
                null,
                null,
                null,
                "created_at DESC",
                "1"
        );
        String payload = null;
        if (cursor.moveToFirst()) {
            payload = cursor.getString(cursor.getColumnIndexOrThrow("payload"));
        }
        cursor.close();
        return payload;
    }

    public String getLatestServerUrl() {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor = database.query(
                LocalDatabaseHelper.TABLE_INIT_DATA,
                new String[]{"server_url"},
                null,
                null,
                null,
                null,
                "created_at DESC",
                "1"
        );
        String url = null;
        if (cursor.moveToFirst()) {
            url = cursor.getString(cursor.getColumnIndexOrThrow("server_url"));
        }
        cursor.close();
        return url;
    }

    public CertificateInfo findCertificateByChip(String chipId) {
        if (chipId == null) {
            return null;
        }
        ensureDataCache();
        for (CertificateInfo info : cachedCertificates) {
            if (chipId.equalsIgnoreCase(info.chipId)) {
                return info;
            }
        }
        return null;
    }

    private void ensureDataCache() {
        String payload = getLatestPayload();
        if (payload == null) {
            cachedCertificates = new ArrayList<>();
            cachedBasicInfo = null;
            cachedEventInfo = null;
            cachedRawPayload = null;
            cachedActiveList = new ArrayList<>();
            return;
        }
        if (payload.equals(cachedRawPayload)) {
            return;
        }
        JSONObject wrapper = parsePayloadOrWrap(payload);
        JSONObject baseInfoObj = wrapper.optJSONObject("baseInfo");
        JSONArray activeUsersArray = wrapper.optJSONArray("activeUsers");
        JSONArray activeListArray = wrapper.optJSONArray("activeList");

        cachedBasicInfo = null;
        if (baseInfoObj != null) {
            cachedBasicInfo = BasicInfoParser.parse(baseInfoObj.toString());
        }

        cachedActiveList = new ArrayList<>();
        if (activeListArray != null) {
            try {
                cachedActiveList = parseActiveListArray(activeListArray);
            } catch (Exception e) {
                cachedActiveList = new ArrayList<>();
            }
        }

        cachedEventInfo = mapEventInfoFromBasic(cachedBasicInfo);
        if (cachedEventInfo == null) {
            cachedEventInfo = EventInfoParser.parse(payload);
        }

        List<CertificateInfo> certificates = new ArrayList<>();
        try {
            certificates.addAll(CertificateParser.parse(payload));
        } catch (JSONException ignore) {
            // ignore legacy formats
        }
        if (activeUsersArray != null) {
            certificates.addAll(ActiveUserParser.parseCertificates(activeUsersArray));
        }
        cachedCertificates = certificates;

        cachedRawPayload = payload;
    }

    private EventInfo mapEventInfoFromBasic(BasicInfo basicInfo) {
        if (basicInfo == null || basicInfo.isEmpty()) {
            return null;
        }
        List<BasicInfo.ActiveModel> actives = basicInfo.getActiveModels();
        if (actives.isEmpty()) {
            return null;
        }
        BasicInfo.ActiveModel active = actives.get(0);
        List<EventSession> sessions = new ArrayList<>();
        List<BasicInfo.ActiveDateModel> dateModels = active.getActiveDateModelList();
        if (!dateModels.isEmpty()) {
            for (int i = 0; i < dateModels.size(); i++) {
                String title = "第" + (i + 1) + "场";
                String date = dateModels.get(i).date;
                sessions.add(new EventSession(title, date, ""));
            }
        }
        List<String> venues = new ArrayList<>();
        for (BasicInfo.ActiveVenueModel venue : basicInfo.getActiveVenues()) {
            if (!TextUtils.isEmpty(venue.venueName)) {
                venues.add(venue.venueName);
            }
        }
        List<String> areas = new ArrayList<>();
        for (BasicInfo.ActiveVenueModel venue : basicInfo.getActiveVenues()) {
            for (BasicInfo.ActiveAreaModel area : venue.getActiveAreaList()) {
                if (!TextUtils.isEmpty(area.areaName)) {
                    areas.add(area.areaName);
                }
            }
        }
        if (areas.isEmpty()) {
            for (BasicInfo.PositionModel position : basicInfo.getPositions()) {
                if (!TextUtils.isEmpty(position.name)) {
                    areas.add(position.name);
                }
            }
        }
        if (sessions.isEmpty() && venues.isEmpty() && areas.isEmpty()) {
            return null;
        }
        return new EventInfo(active.activeName,
                sessions,
                venues,
                areas
        );
    }

    public EventInfo getEventInfo() {
        ensureDataCache();
        return cachedEventInfo;
    }

    public BasicInfo getBasicInfo() {
        ensureDataCache();
        return cachedBasicInfo;
    }

    public List<CertificateInfo> getAllCertificates() {
        ensureDataCache();
        return new ArrayList<>(cachedCertificates);
    }

    public List<BasicInfo.ActiveModel> getActiveList() {
        ensureDataCache();
        return new ArrayList<>(cachedActiveList);
    }

    private List<BasicInfo.ActiveModel> parseActiveListArray(JSONArray array) throws JSONException {
        List<BasicInfo.ActiveModel> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            List<BasicInfo.ActiveDateModel> dates = new ArrayList<>();
            JSONArray dateArray = obj.optJSONArray("activeDateModelList");
            if (dateArray != null) {
                for (int j = 0; j < dateArray.length(); j++) {
                    JSONObject dateObj = dateArray.optJSONObject(j);
                    if (dateObj == null) continue;
                    dates.add(new BasicInfo.ActiveDateModel(dateObj.optString("date")));
                }
            }
            List<BasicInfo.ActiveUnitModel> units = new ArrayList<>();
            JSONArray unitArray = obj.optJSONArray("activeUnitModelList");
            if (unitArray != null) {
                for (int j = 0; j < unitArray.length(); j++) {
                    JSONObject unitObj = unitArray.optJSONObject(j);
                    if (unitObj == null) continue;
                    units.add(new BasicInfo.ActiveUnitModel(unitObj.optString("unitName")));
                }
            }
            BasicInfo.ActiveModel model = new BasicInfo.ActiveModel(
                    obj.optString("id"),
                    obj.optString("status"),
                    obj.optString("activeName"),
                    obj.optString("activeType"),
                    obj.optString("activeVenueId"),
                    obj.optString("licenseUnit"),
                    obj.optString("hostUnit"),
                    obj.optString("activeScale"),
                    obj.optString("securityPerson"),
                    obj.optString("checkPerson"),
                    obj.optString("sign"),
                    obj.optString("avatar"),
                    obj.has("lineStatus") ? obj.optInt("lineStatus") : null,
                    obj.optString("ysActiveId"),
                    obj.optString("accreditationCode"),
                    obj.optString("supervisionId"),
                    obj.optString("signType"),
                    dates,
                    units,
                    obj.optString("signStartTime"),
                    obj.optString("signEndTime"),
                    obj.optString("activeStartTime"),
                    obj.optString("activeEndTime"),
                    obj.optInt("signStatus", 0)
            );
            list.add(model);
        }
        return list;
    }

    private JSONObject loadCurrentPayload() throws JSONException {
        String payload = getLatestPayload();
        if (TextUtils.isEmpty(payload)) {
            return new JSONObject();
        }
        try {
            JSONObject wrapper = new JSONObject(payload);
            if (!wrapper.has("baseInfo") && !wrapper.has("activeUsers") && !wrapper.has("activeList")) {
                JSONObject newWrapper = new JSONObject();
                newWrapper.put("baseInfo", wrapper);
                newWrapper.put("activeUsers", new JSONArray());
                newWrapper.put("activeList", new JSONArray());
                return newWrapper;
            }
            if (!wrapper.has("activeList")) {
                wrapper.put("activeList", new JSONArray());
            }
            return wrapper;
        } catch (JSONException e) {
            JSONObject wrapper = new JSONObject();
            wrapper.put("baseInfo", new JSONObject(payload));
            wrapper.put("activeUsers", new JSONArray());
            wrapper.put("activeList", new JSONArray());
            return wrapper;
        }
    }

    private JSONObject parsePayloadOrWrap(String payload) {
        try {
            JSONObject wrapper = new JSONObject(payload);
            if (wrapper.has("baseInfo") || wrapper.has("activeUsers") || wrapper.has("activeList")) {
                if (!wrapper.has("activeUsers")) {
                    wrapper.put("activeUsers", new JSONArray());
                }
                if (!wrapper.has("activeList")) {
                    wrapper.put("activeList", new JSONArray());
                }
                return wrapper;
            }
            JSONObject newWrapper = new JSONObject();
            newWrapper.put("baseInfo", wrapper);
            newWrapper.put("activeUsers", new JSONArray());
            newWrapper.put("activeList", new JSONArray());
            return newWrapper;
        } catch (JSONException e) {
            JSONObject wrapper = new JSONObject();
            try {
                wrapper.put("baseInfo", new JSONObject(payload));
            } catch (JSONException ignored) {
            }
            try {
                wrapper.put("activeUsers", new JSONArray());
            } catch (JSONException ignored) {
            }
            try {
                wrapper.put("activeList", new JSONArray());
            } catch (JSONException ignored) {
            }
            return wrapper;
        }
    }

    public void close() {
        databaseHelper.close();
    }

    // 添加检查是否有数据的方法
    public boolean hasData() {
        String payload = getLatestPayload();
        return !TextUtils.isEmpty(payload);
    }
}

