package com.largeevent.management.data;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.network.dto.ActiveUserBaseVo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActiveUserParser {

    private static final Map<String, String> IDENTITY_TYPE_LABELS = new HashMap<>();

    static {
        IDENTITY_TYPE_LABELS.put("169", "身份证");
        IDENTITY_TYPE_LABELS.put("1", "身份证");
        IDENTITY_TYPE_LABELS.put("01", "身份证");
        IDENTITY_TYPE_LABELS.put("ID_CARD", "身份证");
    }

    /**
     * 解析单个用户数据为 ActiveUserBaseVo
     */
    public static ActiveUserBaseVo parseActiveUser(JSONObject json) {
        if (json == null) {
            return null;
        }

        ActiveUserBaseVo dto = new ActiveUserBaseVo();
        dto.userId = json.optString("userId");
        dto.certId = json.optString("certId");
        dto.activeId = firstNonEmpty(json.optString("activeId"), dto.activityId);
        dto.registerNumber = firstNonEmpty(
                json.optString("registerNumber"),
                dto.registrationNumber);
        dto.phoneNumber = json.optString("phoneNumber");
        dto.organizationCode = json.optString("organizationCode");
        dto.organization = json.optString("organization");
        // 人证接口标准字段优先
        dto.identityDocumentType = json.optString("identityDocumentType");
        dto.identityDocumentNumber = json.optString("identityDocumentNumber");
        dto.idType = firstNonEmpty(dto.identityDocumentType, json.optString("idType"));
        dto.idNumber = firstNonEmpty(dto.identityDocumentNumber, json.optString("idNumber"));
        dto.registrationNumber = json.optString("registrationNumber");
        dto.passType = json.optString("passType");
        dto.familyNameChinese = json.optString("familyNameChinese");
        dto.givenNameChinese = json.optString("givenNameChinese");
        dto.familyNameInEnglish = json.optString("familyNameInEnglish");
        dto.givenNameInEnglish = json.optString("givenNameInEnglish");
        dto.preferredChineseFamilyName = json.optString("preferredChineseFamilyName");
        dto.preferredChineseGivenName = json.optString("preferredChineseGivenName");
        dto.preferredFamilyName = json.optString("preferredFamilyName");
        dto.preferredGivenName = json.optString("preferredGivenName");
        dto.chipid = json.optString("chipid");
        dto.activityId = json.optString("activityId");
        dto.activityCode = json.optString("activityCode");
        dto.cardReduceImage = json.optString("cardReduceImage");
        dto.number = firstNonEmpty(
                json.optString("number"),
                dto.registerNumber);
        dto.chineseName = resolveChineseNameFromJson(json);
        dto.affiliatedUnitCode = json.optString("affiliatedUnitCode");
        dto.affiliatedUnit = json.optString("affiliatedUnit");
        dto.profile = json.optString("profile");
        dto.birth = json.optString("birth");
        dto.gender = json.optString("gender");
        dto.photo = resolvePhotoUrl(json);
        dto.realStatus = json.optInt("realStatus", 0);
        dto.activeUnitCode = json.optString("activeUnitCode");
        dto.activeUnit = json.optString("activeUnit");
        dto.activeProfile = json.optString("activeProfile");
        dto.passRuleCode = json.optString("passRuleCode");
        dto.tagNo1 = firstNonEmpty(dto.chipid, json.optString("tagNo1"));
        dto.tagNo2 = json.optString("tagNo2");
        dto.subAppTypeCode = json.optString("subAppTypeCode", json.optString("sub_app_type_code"));
        dto.mainAppTypeCode = mapPassType(firstNonEmpty(
                json.optString("mainAppTypeCode"),
                dto.passType));
        dto.cartStatus = json.optString("cartStatus");
        dto.cancelCard = json.optString("cancelCard", "0");
        dto.ctidStatus = json.optString("ctidStatus");
        dto.ctidErrorMsg = json.optString("ctidErrorMsg");
        dto.cardEffectiveDate = json.optString("cardEffectiveDate");
        dto.cardExpirationDate = json.optString("cardExpirationDate");
        dto.cardPublishFlag = json.optInt("cardPublishFlag", 0);
        dto.validBegin = firstNonEmpty(
                dto.cardEffectiveDate,
                json.optString("validBegin"),
                json.optString("validTime"));
        dto.validEnd = firstNonEmpty(dto.cardExpirationDate, json.optString("validEnd"));
        dto.deleted = json.optString("deleted", "0");
        dto.bindStatus = json.optString("bindStatus");

        dto.faceModel = json.optString("faceModel");
        dto.puf = json.optString("puf");
        dto.verifyPhoto = json.optString("verifyPhoto");
        dto.bsStatus = json.optString("bsStatus");
        dto.blackSign = json.optInt("blackSign", 0);
        dto.eventStatus = json.optInt("eventStatus", 0);

        dto.venuePrivileges = json.optString("venuePrivileges");
        dto.areaPrivileges = json.optString("areaPrivileges");
        dto.zonePrivileges = json.optString("zonePrivileges");
        dto.sportPrivileges = json.optString("sportPrivileges", json.optString("sportProject"));
        dto.standPrivileges = json.optString("standPrivileges");
        dto.additionalPrivileges = json.optString("additionalPrivileges");
        dto.effectiveDateOfDayPass = json.optString("effectiveDateOfDayPass");

        return dto;
    }

    /**
     * Gson 反序列化后补齐标准字段（getActiveUser 走 Retrofit 时必须调用）。
     */
    public static void normalizeActiveUserDto(ActiveUserBaseVo dto) {
        if (dto == null) {
            return;
        }
        if (TextUtils.isEmpty(dto.tagNo1) && !TextUtils.isEmpty(dto.chipid)) {
            dto.tagNo1 = dto.chipid.trim();
        }
        if (TextUtils.isEmpty(dto.activeId) && !TextUtils.isEmpty(dto.activityId)) {
            dto.activeId = dto.activityId.trim();
        }
        if (TextUtils.isEmpty(dto.registerNumber) && !TextUtils.isEmpty(dto.registrationNumber)) {
            dto.registerNumber = dto.registrationNumber.trim();
        }
        if (TextUtils.isEmpty(dto.idType) && !TextUtils.isEmpty(dto.identityDocumentType)) {
            dto.idType = dto.identityDocumentType.trim();
        }
        if (TextUtils.isEmpty(dto.idNumber) && !TextUtils.isEmpty(dto.identityDocumentNumber)) {
            dto.idNumber = dto.identityDocumentNumber.trim();
        }
        if (TextUtils.isEmpty(dto.mainAppTypeCode) && !TextUtils.isEmpty(dto.passType)) {
            dto.mainAppTypeCode = mapPassType(dto.passType);
        }
        if (TextUtils.isEmpty(dto.chineseName)) {
            dto.chineseName = resolveChineseNameFromDto(dto);
        }
        if (TextUtils.isEmpty(dto.number)) {
            dto.number = firstNonEmpty(dto.registrationNumber, dto.registerNumber);
        }
        if (TextUtils.isEmpty(dto.photo) || !isHttpUrl(dto.photo)) {
            String resolvedPhoto = resolveHttpPhotoUrl(dto);
            if (!TextUtils.isEmpty(resolvedPhoto)) {
                dto.photo = resolvedPhoto;
            }
        }
        syncCardValidityFields(dto);
    }

    /** 有效期以 cardEffectiveDate / cardExpirationDate 为准，并回写兼容字段。 */
    public static void syncCardValidityFields(ActiveUserBaseVo dto) {
        if (dto == null) {
            return;
        }
        String effective = resolveCardEffectiveDate(dto);
        String expiration = resolveCardExpirationDate(dto);
        if (!TextUtils.isEmpty(effective)) {
            dto.cardEffectiveDate = effective;
            dto.validBegin = effective;
        }
        if (!TextUtils.isEmpty(expiration)) {
            dto.cardExpirationDate = expiration;
            dto.validEnd = expiration;
        }
    }

    @Nullable
    public static String resolveCardEffectiveDate(ActiveUserBaseVo dto) {
        if (dto == null) {
            return null;
        }
        return firstNonEmpty(dto.cardEffectiveDate, dto.validBegin);
    }

    @Nullable
    public static String resolveCardExpirationDate(ActiveUserBaseVo dto) {
        if (dto == null) {
            return null;
        }
        return firstNonEmpty(dto.cardExpirationDate, dto.validEnd);
    }

    /**
     * TP 日卡「当日通行」判定日期：优先 effectiveDateOfDayPass，缺失时回退 cardEffectiveDate。
     * getActiveUser 常只返回 cardEffectiveDate / cardExpirationDate，不单独下发日卡字段。
     */
    @Nullable
    public static String resolveTpDayPassDate(ActiveUserBaseVo dto) {
        if (dto == null) {
            return null;
        }
        String dayPass = dto.effectiveDateOfDayPass;
        if (dayPass != null && !dayPass.trim().isEmpty()) {
            return dayPass.trim();
        }
        return resolveCardEffectiveDate(dto);
    }

    /**
     * 从 ActiveUserBaseVo 构建 CertificateInfo
     */
    public static CertificateInfo buildCertificateInfo(ActiveUserBaseVo dto, String fallbackChipId) {
        if (dto == null) {
            return null;
        }
        normalizeActiveUserDto(dto);

        // 按证件号查询时接口常返回空 chipId，不能因此判定无证件
        String idNumber = firstNonEmpty(dto.identityDocumentNumber, dto.idNumber);
        String chipId = firstNonEmpty(dto.chipid, dto.tagNo1, fallbackChipId, dto.certId, idNumber);
        if (TextUtils.isEmpty(chipId)
                && TextUtils.isEmpty(dto.certId)
                && TextUtils.isEmpty(idNumber)
                && TextUtils.isEmpty(dto.chineseName)) {
            return null;
        }

        String idTypeRaw = firstNonEmpty(dto.identityDocumentType, dto.idType);
        String idTypeDisplay = resolveIdentityDocumentTypeLabel(idTypeRaw);

        CertificateInfo.Builder builder = new CertificateInfo.Builder()
                .setCertId(dto.certId)
                .setName(dto.chineseName)
                .setNumber(firstNonEmpty(dto.registrationNumber, dto.registerNumber, dto.number))
                .setIdentityDocumentType(idTypeDisplay)
                .setIdentityDocumentNumber(idNumber)
                .setDocumentType(idTypeDisplay)
                .setChipId(chipId)
                .setCardSerial(idNumber)
                .setValidFrom(resolveCardEffectiveDate(dto))
                .setValidTo(resolveCardExpirationDate(dto))
                .setNeedBinding("02".equals(dto.mainAppTypeCode))
                .setBound("1".equals(dto.bindStatus))
                .setRealNameRequired(dto.realStatus == 1)
                .setPhotoUrl(resolveCertificateDisplayPhoto(dto))
                .setPassRuleCode(dto.passRuleCode)
                .setVenuePrivileges(dto.venuePrivileges)
                .setAreaPrivileges(dto.areaPrivileges)
                .setZonePrivileges(dto.zonePrivileges)
                .setSportPrivileges(dto.sportPrivileges)
                .setEffectiveDateOfDayPass(resolveTpDayPassDate(dto))
                .setActivityId(dto.activityId)
                .setActivityCode(dto.activityCode);

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
            if (obj == null) {
                continue;
            }
            ActiveUserBaseVo dto = parseActiveUser(obj);
            String chipId = obj.optString("chipid", obj.optString("tagNo2"));
            CertificateInfo info = buildCertificateInfo(dto, chipId);
            if (info == null) {
                continue;
            }
            CertificateInfo.Builder builder = new CertificateInfo.Builder()
                    .setCertId(info.certId)
                    .setName(info.name)
                    .setNumber(info.number)
                    .setDocumentType(info.documentType)
                    .setIdentityDocumentType(info.identityDocumentType)
                    .setIdentityDocumentNumber(info.identityDocumentNumber)
                    .setChipId(info.chipId)
                    .setCardSerial(info.cardSerial)
                    .setValidFrom(info.validFrom)
                    .setValidTo(info.validTo)
                    .setNeedBinding(info.isNeedBinding())
                    .setBound(info.isBound())
                    .setRealNameRequired(info.isRealNameRequired())
                    .setPhotoUrl(info.photoUrl)
                    .setPassRuleCode(info.passRuleCode)
                    .setVenuePrivileges(info.venuePrivileges)
                    .setAreaPrivileges(info.areaPrivileges)
                    .setZonePrivileges(info.zonePrivileges)
                    .setSportPrivileges(info.sportPrivileges)
                    .setEffectiveDateOfDayPass(info.effectiveDateOfDayPass);
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
            if (obj == null) {
                continue;
            }
            if (isVehicle(obj)) {
                stats.vehicleCount++;
            } else {
                stats.personCount++;
            }
        }
        return stats;
    }

    /**
     * passType（MP/TP/VP）与 mainAppTypeCode（01/02/03）互认。
     */
    public static String mapPassType(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            case "MP":
            case "01":
                return "01";
            case "TP":
            case "02":
                return "02";
            case "VP":
            case "03":
                return "03";
            default:
                return raw.trim();
        }
    }

    public static String resolveIdentityDocumentTypeLabel(String code) {
        if (TextUtils.isEmpty(code)) {
            return null;
        }
        String trimmed = code.trim();
        // 已是中文等非纯编码描述时直接展示
        if (!trimmed.matches("^[0-9A-Za-z_-]+$")) {
            return trimmed;
        }
        String label = IDENTITY_TYPE_LABELS.get(trimmed);
        if (!TextUtils.isEmpty(label)) {
            return label;
        }
        label = IDENTITY_TYPE_LABELS.get(trimmed.toUpperCase(Locale.ROOT));
        return !TextUtils.isEmpty(label) ? label : trimmed;
    }

    /**
     * 姓名：中文姓+中文名优先，其次英文姓+英文名。
     */
    public static String resolveChineseNameFromDto(ActiveUserBaseVo dto) {
        if (dto == null) {
            return "";
        }
        String fromChinese = joinName(dto.familyNameChinese, dto.givenNameChinese);
        if (!TextUtils.isEmpty(fromChinese)) {
            return fromChinese;
        }
        if (!TextUtils.isEmpty(dto.chineseName)) {
            return dto.chineseName.trim();
        }
        String fromPreferredChinese = joinName(
                dto.preferredChineseFamilyName, dto.preferredChineseGivenName);
        if (!TextUtils.isEmpty(fromPreferredChinese)) {
            return fromPreferredChinese;
        }
        String fromEnglish = joinName(dto.familyNameInEnglish, dto.givenNameInEnglish);
        if (!TextUtils.isEmpty(fromEnglish)) {
            return fromEnglish;
        }
        return joinName(dto.preferredFamilyName, dto.preferredGivenName);
    }

    /**
     * 查验详情证件照：优先 getActiveUser.verifyPhoto（URL 或 data:image base64）。
     */
    @Nullable
    public static String resolveCertificateDisplayPhoto(@Nullable ActiveUserBaseVo dto) {
        if (dto == null) {
            return null;
        }
        if (!TextUtils.isEmpty(dto.verifyPhoto)) {
            return dto.verifyPhoto.trim();
        }
        String httpPhoto = resolveHttpPhotoUrl(dto);
        if (!TextUtils.isEmpty(httpPhoto)) {
            return httpPhoto;
        }
        String fallback = firstNonEmpty(dto.cardReduceImage, dto.photo);
        return TextUtils.isEmpty(fallback) ? null : fallback.trim();
    }

    /** 人脸比对等仅需 HTTP 头像 URL 的场景 */
    @Nullable
    public static String resolveFaceMatchCertPhotoUrl(@Nullable ActiveUserBaseVo dto) {
        return resolveHttpPhotoUrl(dto);
    }

    @Nullable
    private static String resolveHttpPhotoUrl(@Nullable ActiveUserBaseVo dto) {
        if (dto == null) {
            return null;
        }
        if (!TextUtils.isEmpty(dto.cardReduceImage) && isHttpUrl(dto.cardReduceImage)) {
            return dto.cardReduceImage.trim();
        }
        if (!TextUtils.isEmpty(dto.verifyPhoto) && isHttpUrl(dto.verifyPhoto)) {
            return dto.verifyPhoto.trim();
        }
        if (!TextUtils.isEmpty(dto.photo) && isHttpUrl(dto.photo)) {
            return dto.photo.trim();
        }
        return null;
    }

    private static String resolvePhotoUrlFromDto(ActiveUserBaseVo dto) {
        String display = resolveCertificateDisplayPhoto(dto);
        return display == null ? "" : display;
    }

    private static boolean isHttpUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        String u = url.trim();
        return u.startsWith("http://") || u.startsWith("https://");
    }

    private static String resolveChineseNameFromJson(JSONObject json) {
        String fromChinese = joinName(
                json.optString("familyNameChinese"),
                json.optString("givenNameChinese"));
        if (!TextUtils.isEmpty(fromChinese)) {
            return fromChinese;
        }

        String direct = firstNonEmpty(
                json.optString("chineseName"),
                json.optString("name"));
        if (!TextUtils.isEmpty(direct)) {
            return direct.trim();
        }

        String fromPreferredChinese = joinName(
                json.optString("preferredChineseFamilyName"),
                json.optString("preferredChineseGivenName"));
        if (!TextUtils.isEmpty(fromPreferredChinese)) {
            return fromPreferredChinese;
        }

        String fromEnglish = joinName(
                json.optString("familyNameInEnglish"),
                json.optString("givenNameInEnglish"));
        if (!TextUtils.isEmpty(fromEnglish)) {
            return fromEnglish;
        }

        return joinName(
                json.optString("preferredFamilyName"),
                json.optString("preferredGivenName"));
    }

    private static String resolvePhotoUrl(JSONObject json) {
        ActiveUserBaseVo tmp = new ActiveUserBaseVo();
        tmp.cardReduceImage = json.optString("cardReduceImage");
        tmp.verifyPhoto = json.optString("verifyPhoto");
        tmp.photo = json.optString("photo");
        return resolvePhotoUrlFromDto(tmp);
    }

    private static String joinName(String family, String given) {
        String f = family == null ? "" : family.trim();
        String g = given == null ? "" : given.trim();
        if (TextUtils.isEmpty(f) && TextUtils.isEmpty(g)) {
            return "";
        }
        if (TextUtils.isEmpty(f)) {
            return g;
        }
        if (TextUtils.isEmpty(g)) {
            return f;
        }
        return f + g;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isVehicle(JSONObject obj) {
        String subType = obj.optString("subAppTypeCode", obj.optString("sub_app_type_code"));
        if (!TextUtils.isEmpty(subType)) {
            String lower = subType.toLowerCase(Locale.ROOT);
            if (lower.contains("car") || lower.contains("veh")) {
                return true;
            }
        }
        String idType = firstNonEmpty(
                obj.optString("idType"),
                obj.optString("identityDocumentType"));
        if (!TextUtils.isEmpty(idType) && idType.contains("车")) {
            return true;
        }
        String profile = obj.optString("activeProfile");
        if (!TextUtils.isEmpty(profile) && profile.contains("车")) {
            return true;
        }
        String registerNumber = firstNonEmpty(
                obj.optString("registerNumber"),
                obj.optString("registrationNumber"));
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
