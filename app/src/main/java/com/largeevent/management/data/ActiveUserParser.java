package com.largeevent.management.data;

import android.text.TextUtils;

import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.network.dto.ActiveUserBaseDTO;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActiveUserParser {

    /**
     * 解析单个用户数据为 ActiveUserBaseDTO
     */
    public static ActiveUserBaseDTO parseActiveUser(JSONObject json) {
        if (json == null) {
            return null;
        }

        ActiveUserBaseDTO dto = new ActiveUserBaseDTO();
        dto.userId = json.optString("userId");
        dto.certId = json.optString("certId");
        dto.activeId = json.optString("activeId");
        dto.registerNumber = json.optString("registerNumber");
        dto.phoneNumber = json.optString("phoneNumber");
        dto.organizationCode = json.optString("organizationCode");
        dto.organization = json.optString("organization");
        dto.idType = json.optString("idType");
        dto.idNumber = json.optString("idNumber");
        dto.number = json.optString("number");
        dto.chineseName = json.optString("chineseName");
        dto.affiliatedUnitCode = json.optString("affiliatedUnitCode");
        dto.affiliatedUnit = json.optString("affiliatedUnit");
        dto.profile = json.optString("profile");
        dto.birth = json.optString("birth");
        dto.gender = json.optString("gender");
        dto.photo = json.optString("photo");
        dto.realStatus = json.optInt("realStatus", 0);
        dto.activeUnitCode = json.optString("activeUnitCode");
        dto.activeUnit = json.optString("activeUnit");
        dto.activeProfile = json.optString("activeProfile");
        dto.passRuleCode = json.optString("passRuleCode");
        dto.tagNo1 = json.optString("chipid");
        dto.tagNo2 = json.optString("tagNo2");
        dto.subAppTypeCode = json.optString("subAppTypeCode", json.optString("sub_app_type_code"));
        dto.mainAppTypeCode = json.optString("mainAppTypeCode");
        dto.cartStatus = json.optString("cartStatus");
        dto.cancelCard = json.optString("cancelCard", "0");
        dto.ctidStatus = json.optString("ctidStatus");
        dto.ctidErrorMsg = json.optString("ctidErrorMsg");
        dto.validBegin = json.optString("validBegin");
        dto.validEnd = json.optString("validEnd");
        dto.deleted = json.optString("deleted", "0");
        dto.bindStatus = json.optString("bindStatus");

        // 文档新增字段
        dto.faceModel = json.optString("faceModel");
        dto.puf = json.optString("puf");
        dto.verifyPhoto = json.optString("verifyPhoto");
        dto.bsStatus = json.optString("bsStatus");
        dto.blackSign = json.optInt("blackSign", 0);
        dto.eventStatus = json.optInt("eventStatus", 0);

        // 权限相关字段
        dto.venuePrivileges = json.optString("venuePrivileges");
        dto.areaPrivileges = json.optString("areaPrivileges");
        dto.zonePrivileges = json.optString("zonePrivileges");
        dto.standPrivileges = json.optString("standPrivileges");
        dto.additionalPrivileges = json.optString("additionalPrivileges");
        dto.effectiveDateOfDayPass = json.optString("effectiveDateOfDayPass");

        return dto;
    }

    /**
     * 从 ActiveUserBaseDTO 构建 CertificateInfo
     */
    public static CertificateInfo buildCertificateInfo(ActiveUserBaseDTO dto, String fallbackChipId) {
        if (dto == null) {
            return null;
        }

        String chipId = !TextUtils.isEmpty(dto.tagNo1) ? dto.tagNo1 : fallbackChipId;
        if (TextUtils.isEmpty(chipId)) {
            return null;
        }

        String passRuleCode = dto.passRuleCode;

        CertificateInfo.Builder builder = new CertificateInfo.Builder()
                .setCertId(dto.certId)
                .setName(dto.chineseName)
                .setNumber(dto.number)
                .setDocumentType(dto.idType)
                .setChipId(chipId)
                .setCardSerial(!TextUtils.isEmpty(dto.registerNumber) ? dto.registerNumber : dto.idNumber)
                .setValidFrom(dto.validBegin)
                .setValidTo(dto.validEnd)
                .setNeedBinding("02".equals(dto.mainAppTypeCode))
                .setBound(false)
                .setRealNameRequired(false)
                .setPhotoUrl(dto.photo)
                .setPassRuleCode(passRuleCode)
                .setVenuePrivileges(dto.venuePrivileges)
                .setAreaPrivileges(dto.areaPrivileges)
                .setZonePrivileges(dto.zonePrivileges)
                .setEffectiveDateOfDayPass(dto.effectiveDateOfDayPass);

        return builder.build();
    }

    /**
     * 解析证书列表（旧逻辑，保留兼容）
     */
    static List<CertificateInfo> parseCertificates(JSONArray array) {
        List<CertificateInfo> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            String chipId = obj.optString("chipid", obj.optString("tagNo2"));
            if (TextUtils.isEmpty(chipId)) {
                continue;
            }
            CertificateInfo.Builder builder = new CertificateInfo.Builder()
                    .setName(obj.optString("chineseName", obj.optString("name")))
                    .setDocumentType(obj.optString("idType"))
                    .setChipId(chipId)
                    .setCardSerial(obj.optString("registerNumber", obj.optString("idNumber")))
                    .setValidFrom("")
                    .setValidTo("")
                    .setNeedBinding(false)
                    .setBound(true)
                    .setRealNameRequired(obj.optInt("realStatus", 1) == 1);
            String passRule = obj.optString("passRuleCode");
            if (!TextUtils.isEmpty(passRule)) {
                builder.addPermission(passRule);
            } else {
                builder.addPermission("ALL");
            }
            result.add(builder.build());
        }
        return result;
    }

    public static ActiveUserStats calculateStats(JSONArray array) {
        ActiveUserStats stats = new ActiveUserStats();
        if (array == null) {
            return stats;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            if (isVehicle(obj)) {
                stats.vehicleCount++;
            } else {
                stats.personCount++;
            }
        }
        return stats;
    }

    private static boolean isVehicle(JSONObject obj) {
        String subType = obj.optString("subAppTypeCode", obj.optString("sub_app_type_code"));
        if (!TextUtils.isEmpty(subType)) {
            String lower = subType.toLowerCase();
            if (lower.contains("car") || lower.contains("veh")) {
                return true;
            }
        }
        String idType = obj.optString("idType");
        if (!TextUtils.isEmpty(idType) && idType.contains("车")) {
            return true;
        }
        String profile = obj.optString("activeProfile");
        if (!TextUtils.isEmpty(profile) && profile.contains("车")) {
            return true;
        }
        String registerNumber = obj.optString("registerNumber");
        if (!TextUtils.isEmpty(registerNumber)) {
            boolean hasLetter = false;
            for (int i = 0; i < registerNumber.length(); i++) {
                if (Character.isLetter(registerNumber.charAt(i))) {
                    hasLetter = true;
                    break;
                }
            }
            if (hasLetter && registerNumber.length() >= 5) {
                return true;
            }
        }
        return false;
    }

    public static class ActiveUserStats {
        public int personCount;
        public int vehicleCount;
    }
}
