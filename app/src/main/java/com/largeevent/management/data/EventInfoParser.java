package com.largeevent.management.data;

import android.text.TextUtils;

import com.largeevent.management.model.EventInfo;
import com.largeevent.management.model.EventSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class EventInfoParser {

    static EventInfo parse(String payload) {
        if (TextUtils.isEmpty(payload)) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(payload);
            JSONObject event = root.optJSONObject("event");
            if (event == null) {
                event = root.optJSONObject("eventInfo");
            }
            if (event == null) {
                return null;
            }
            String name = event.optString("name",
                    event.optString("eventName", "未设置活动"));

            List<EventSession> sessions = new ArrayList<>();
            JSONArray sessionArray = event.optJSONArray("sessions");
            if (sessionArray == null) {
                sessionArray = event.optJSONArray("sessionList");
            }
            if (sessionArray != null) {
                for (int i = 0; i < sessionArray.length(); i++) {
                    JSONObject obj = sessionArray.optJSONObject(i);
                    if (obj == null) continue;
                    String title = obj.optString("title",
                            obj.optString("name", "场次" + (i + 1)));
                    String start = obj.optString("start",
                            obj.optString("startTime", ""));
                    String end = obj.optString("end",
                            obj.optString("endTime", ""));
                    sessions.add(new EventSession(title, start, end));
                }
            }

            List<String> venuePermissions = new ArrayList<>();
            JSONArray venueArray = event.optJSONArray("venuePermissions");
            if (venueArray == null) {
                venueArray = event.optJSONArray("venueRights");
            }
            if (venueArray != null) {
                for (int i = 0; i < venueArray.length(); i++) {
                    String text = venueArray.optString(i);
                    if (!TextUtils.isEmpty(text)) {
                        venuePermissions.add(text);
                    }
                }
            }

            List<String> areaPermissions = new ArrayList<>();
            JSONArray areaArray = event.optJSONArray("areaPermissions");
            if (areaArray == null) {
                areaArray = event.optJSONArray("areaRights");
            }
            if (areaArray != null) {
                for (int i = 0; i < areaArray.length(); i++) {
                    String text = areaArray.optString(i);
                    if (!TextUtils.isEmpty(text)) {
                        areaPermissions.add(text);
                    }
                }
            }

            if (sessions.isEmpty() && venuePermissions.isEmpty() && areaPermissions.isEmpty()) {
                return null;
            }

            return new EventInfo(name, sessions, venuePermissions, areaPermissions);
        } catch (JSONException e) {
            return null;
        }
    }
}


