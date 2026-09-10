package com.largeevent.management.data;

import android.text.TextUtils;

import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.BasicInfo.ActiveAreaModel;
import com.largeevent.management.model.BasicInfo.ActiveDateModel;
import com.largeevent.management.model.BasicInfo.ActiveModel;
import com.largeevent.management.model.BasicInfo.ActiveUnitModel;
import com.largeevent.management.model.BasicInfo.ActiveVenueModel;
import com.largeevent.management.model.BasicInfo.CartTypeModel;
import com.largeevent.management.model.BasicInfo.CertTypeModel;
import com.largeevent.management.model.BasicInfo.PassRuleModel;
import com.largeevent.management.model.BasicInfo.PositionModel;
import com.largeevent.management.model.BasicInfo.LocationInfo;
import com.largeevent.management.model.BasicInfo.MatrixAuthInfo;
import com.largeevent.management.model.BasicInfo.EpidemicInfo;
import com.largeevent.management.model.BasicInfo.VenueInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class BasicInfoParser {

    static BasicInfo parse(String payload) throws JSONException {
        if (TextUtils.isEmpty(payload)) {
            return new BasicInfo(null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
        JSONObject root = new JSONObject(payload);
        if (root.has("data")) {
            root = root.optJSONObject("data");
        }
        if (root == null) {
            return new BasicInfo(null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
        
        List<ActiveModel> activeModels = parseActiveModels(root.optJSONArray("activeModelList"));
        List<ActiveVenueModel> venueModels = parseActiveVenues(root.optJSONArray("activeVenueModelList"));
        List<PositionModel> positions = parsePositions(root.optJSONArray("positionModelList"));
        List<PassRuleModel> passRules = parsePassRules(root.optJSONArray("passRuleModelList"));
        List<CartTypeModel> cartTypes = parseCartTypes(root.optJSONArray("cartTypeModelList"));
        List<CertTypeModel> personCertTypes = parseCertTypes(root.optJSONArray("personCertTypeList"));
        List<CertTypeModel> carCertTypes = parseCertTypes(root.optJSONArray("carCertTypeList"));
        
        List<LocationInfo> locationInfoList = parseLocationInfoList(root.optJSONArray("locationInfoList"));
        List<MatrixAuthInfo> matrixAuthInfoList = parseMatrixAuthInfoList(root.optJSONArray("matrixAuthInfoList"));
        List<EpidemicInfo> epidemicInfoList = parseEpidemicInfoList(root.optJSONArray("epidemicInfoList"));
        List<VenueInfo> venueInfoList = parseVenueInfoList(root.optJSONArray("venueInfoList"));
        List<VenueInfo> personCertZoneList = parseVenueInfoList(root.optJSONArray("personCertZoneList"));
        List<VenueInfo> personCertAreaList = parseVenueInfoList(root.optJSONArray("personCertAreaList"));
        
        return new BasicInfo(activeModels, venueModels, positions, passRules, cartTypes, 
                personCertTypes, carCertTypes, locationInfoList, matrixAuthInfoList, epidemicInfoList,
                venueInfoList, personCertZoneList, personCertAreaList);
    }

    private static List<ActiveModel> parseActiveModels(JSONArray array) {
        List<ActiveModel> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            List<ActiveDateModel> dates = new ArrayList<>();
            JSONArray dateArray = obj.optJSONArray("activeDateModelList");
            if (dateArray != null) {
                for (int j = 0; j < dateArray.length(); j++) {
                    JSONObject dateObj = dateArray.optJSONObject(j);
                    if (dateObj == null) continue;
                    dates.add(new ActiveDateModel(dateObj.optString("date")));
                }
            }
            List<ActiveUnitModel> units = new ArrayList<>();
            JSONArray unitArray = obj.optJSONArray("activeUnitModelList");
            if (unitArray != null) {
                for (int j = 0; j < unitArray.length(); j++) {
                    JSONObject unitObj = unitArray.optJSONObject(j);
                    if (unitObj == null) continue;
                    units.add(new ActiveUnitModel(unitObj.optString("unitName")));
                }
            }
            ActiveModel model = new ActiveModel(
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

    private static List<ActiveVenueModel> parseActiveVenues(JSONArray array) {
        List<ActiveVenueModel> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            List<ActiveAreaModel> areas = new ArrayList<>();
            JSONArray areaArray = obj.optJSONArray("activeAreaList");
            if (areaArray != null) {
                for (int j = 0; j < areaArray.length(); j++) {
                    JSONObject areaObj = areaArray.optJSONObject(j);
                    if (areaObj == null) continue;
                    areas.add(new ActiveAreaModel(
                            areaObj.optString("id"),
                            areaObj.optString("areaName", areaObj.optString("name")),
                            areaObj.optString("areaCode", areaObj.optString("code"))
                    ));
                }
            }
            ActiveVenueModel model = new ActiveVenueModel(
                    obj.optString("id"),
                    obj.optString("activeVenueType"),
                    obj.optString("activeVenueSort"),
                    obj.optString("venueCode"),
                    obj.optString("gis"),
                    obj.optString("venueName"),
                    obj.optString("venuePicture"),
                    obj.optString("address"),
                    areas
            );
            list.add(model);
        }
        return list;
    }

    private static List<PositionModel> parsePositions(JSONArray array) {
        List<PositionModel> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new PositionModel(
                    obj.optString("id"),
                    obj.optString("name"),
                    obj.optString("positionCode"),
                    obj.optString("positionType"),
                    obj.optString("positionDesc"),
                    obj.optString("parentPositionId")
            ));
        }
        return list;
    }

    private static List<PassRuleModel> parsePassRules(JSONArray array) {
        List<PassRuleModel> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new PassRuleModel(
                    obj.optString("id"),
                    obj.optString("code"),
                    obj.optString("passPositionCode"),
                    obj.optString("timeType"),
                    obj.optString("timeDesc")
            ));
        }
        return list;
    }

    private static List<CartTypeModel> parseCartTypes(JSONArray array) {
        List<CartTypeModel> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new CartTypeModel(
                    obj.optString("id"),
                    obj.optString("positionCode"),
                    obj.optString("subAppTypeCode")
            ));
        }
        return list;
    }

    private static List<CertTypeModel> parseCertTypes(JSONArray array) {
        List<CertTypeModel> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new CertTypeModel(
                    obj.optString("sortNumber"),
                    obj.optString("dictType"),
                    obj.optString("isLocked"),
                    obj.optString("dictValue"),
                    obj.optString("dictCode")
            ));
        }
        return list;
    }

    private static List<LocationInfo> parseLocationInfoList(JSONArray array) {
        List<LocationInfo> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new LocationInfo(
                    obj.optString("locationId"),
                    obj.optString("parentId"),
                    obj.optString("locationNo"),
                    obj.optString("locationName"),
                    obj.optString("locationType"),
                    obj.optString("locationDesc"),
                    obj.optString("activityId"),
                    obj.optString("eqpId"),
                    obj.optString("activityCode"),
                    obj.optInt("moduleType", 0)
            ));
        }
        return list;
    }

    private static List<MatrixAuthInfo> parseMatrixAuthInfoList(JSONArray array) {
        List<MatrixAuthInfo> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new MatrixAuthInfo(
                    obj.optString("authId"),
                    obj.optString("name"),
                    obj.optString("number"),
                    obj.optString("venue"),
                    obj.optString("venueVal"),
                    obj.optString("sportProject"),
                    obj.optString("sportProjectVal"),
                    obj.optString("venueArea"),
                    obj.optString("venueAreaVal"),
                    obj.optString("venuePartition"),
                    obj.optString("venuePartitionVal"),
                    obj.optString("seat"),
                    obj.optString("seatVal"),
                    obj.optString("other"),
                    obj.optString("otherVal"),
                    obj.optString("park"),
                    obj.optString("parkVal"),
                    obj.optString("securityColor"),
                    obj.optString("securityColorVal"),
                    obj.has("type") ? obj.optInt("type") : null,
                    obj.optString("eqpId"),
                    obj.optString("serverUpdateTime"),
                    obj.optString("activityId"),
                    obj.optString("activityCode")
            ));
        }
        return list;
    }

    private static List<EpidemicInfo> parseEpidemicInfoList(JSONArray array) {
        List<EpidemicInfo> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new EpidemicInfo(
                    obj.optString("epidemicId"),
                    obj.optString("registrationNumber"),
                    obj.has("isDetectedStatus") ? obj.optInt("isDetectedStatus") : null,
                    obj.has("closeContactStatus") ? obj.optInt("closeContactStatus") : null,
                    obj.has("positiveStatus") ? obj.optInt("positiveStatus") : null,
                    obj.optString("pushTime"),
                    obj.optString("receiveTime"),
                    obj.optString("activityId"),
                    obj.optString("activityCode"),
                    obj.optString("rfu"),
                    obj.optString("eqpId")
            ));
        }
        return list;
    }

    private static List<VenueInfo> parseVenueInfoList(JSONArray array) {
        List<VenueInfo> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            list.add(new VenueInfo(
                    obj.optString("id"),
                    obj.optString("createBy"),
                    obj.optString("createTime"),
                    obj.optString("updateBy"),
                    obj.optString("updateTime"),
                    obj.has("isDeleted") ? obj.optInt("isDeleted") : null,
                    obj.optString("remark"),
                    obj.optString("dictType"),
                    obj.optString("dictCode"),
                    obj.optString("dictValue"),
                    obj.has("sortNumber") ? obj.optInt("sortNumber") : null,
                    obj.has("isLocked") ? obj.optInt("isLocked") : null
            ));
        }
        return list;
    }
}
