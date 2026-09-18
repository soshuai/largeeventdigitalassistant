package com.largeevent.management.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;
import com.largeevent.management.network.dto.ActiveUserBaseVo;
import com.largeevent.management.network.dto.ReceiveCheckPersonAo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 按 receiveCheckPerson 接口文档组装上传体。
 */
public final class ReceiveCheckPersonBuilder {

    private static final String EPIDEMIC_DISABLED = "2";

    private ReceiveCheckPersonBuilder() {
    }

    public static ReceiveCheckPersonAo build(
            Context context,
            VerificationResult result,
            @Nullable ActiveUserBaseVo user,
            @Nullable CertificateInfo info,
            @Nullable BasicInfo basicInfo,
            @Nullable String photoBase64) {
        ReceiveCheckPersonAo dto = new ReceiveCheckPersonAo();
        String activeId = AppPreferences.getLastActiveId(context);
        if (user != null) {
            ActiveUserParser.normalizeActiveUserDto(user);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String now = sdf.format(new Date());

        dto.entryId = UUID.randomUUID().toString().replace("-", "");
        dto.verifyTime = now;
        dto.createTime = now;
        dto.verifyStatus = mapVerifyStatus(result);
        dto.direction = result != null && result.type == VerificationResultType.PASS ? 0 : 1;

        dto.equipmentId = AppPreferences.getDeviceCode(context);
        dto.equipmentName = android.os.Build.MODEL;

        // 人证核验上报使用人证模块的位置/分区
        dto.locationId = AppPreferences.getSelectedLocationId(
                context, activeId, ModuleType.PERSON);
        dto.zoneLocationId = AppPreferences.getSelectedZoneId(
                context, activeId, ModuleType.PERSON);
        dto.locationName = resolveLocationName(basicInfo, dto.locationId);
        dto.zoneLocationName = resolveLocationName(basicInfo, dto.zoneLocationId);

        // 使用 App 内保存的当前选中活动 ID，不用证件接口返回的 activityId
        if (!TextUtils.isEmpty(activeId)) {
            dto.activityId = activeId.trim();
        }

        dto.name = resolvePersonName(user, info);
        dto.cardId = user != null ? user.certId : (info != null ? info.certId : "");
        dto.cardNumber = resolveCardNumber(user, info);

        applyEpidemicStatus(dto, basicInfo, dto.cardNumber);

        // 核验页有现场拍照时上传 imageName / remark(Base64)，与是否执行人脸比对无关
        if (!TextUtils.isEmpty(photoBase64)) {
            dto.imageName = buildImageName(dto.activityId, dto.cardId, now);
            dto.remark = photoBase64;
        }

        return dto;
    }

    private static int mapVerifyStatus(@Nullable VerificationResult result) {
        if (result == null) {
            return VerificationResultType.NOT_DETECTED.code;
        }
        int code = result.type.code;
        // 文档未定义 10-未绑定，归入 9-请检查证件
        if (code == VerificationResultType.UNBOUND.code) {
            return VerificationResultType.FACE_MISMATCH.code;
        }
        if (code == VerificationResultType.BIND_REQUIRED.code) {
            return VerificationResultType.INVALID_CERT.code;
        }
        return code;
    }

    @Nullable
    private static String resolvePersonName(
            @Nullable ActiveUserBaseVo user, @Nullable CertificateInfo info) {
        if (user != null && !TextUtils.isEmpty(user.chineseName)) {
            return user.chineseName.trim();
        }
        if (info != null && !TextUtils.isEmpty(info.name)) {
            return info.name.trim();
        }
        return "";
    }

    private static String resolveCardNumber(
            @Nullable ActiveUserBaseVo user, @Nullable CertificateInfo info) {
        if (user != null && !TextUtils.isEmpty(user.registrationNumber)) {
            return user.registrationNumber.trim();
        }
        if (info != null && !TextUtils.isEmpty(info.number)) {
            return info.number.trim();
        }
        return "";
    }

    private static void applyEpidemicStatus(
            ReceiveCheckPersonAo dto,
            @Nullable BasicInfo basicInfo,
            @Nullable String cardNumber) {
        dto.isDetectedStatus = EPIDEMIC_DISABLED;
        dto.closeContactStatus = EPIDEMIC_DISABLED;
        dto.positiveStatus = EPIDEMIC_DISABLED;
        if (basicInfo == null || TextUtils.isEmpty(cardNumber)) {
            return;
        }
        List<BasicInfo.EpidemicInfo> list = basicInfo.getEpidemicInfoList();
        if (list == null) {
            return;
        }
        for (BasicInfo.EpidemicInfo epidemic : list) {
            if (epidemic == null || epidemic.registrationNumber == null) {
                continue;
            }
            if (!cardNumber.equals(epidemic.registrationNumber.trim())) {
                continue;
            }
            if (epidemic.isDetectedStatus != null) {
                dto.isDetectedStatus = String.valueOf(epidemic.isDetectedStatus);
            }
            if (epidemic.closeContactStatus != null) {
                dto.closeContactStatus = String.valueOf(epidemic.closeContactStatus);
            }
            if (epidemic.positiveStatus != null) {
                dto.positiveStatus = String.valueOf(epidemic.positiveStatus);
            }
            break;
        }
    }

    @Nullable
    private static String resolveLocationName(@Nullable BasicInfo basicInfo, @Nullable String locationId) {
        if (basicInfo == null || TextUtils.isEmpty(locationId)) {
            return "";
        }
        List<BasicInfo.LocationInfo> locations = basicInfo.getLocationInfoList();
        if (locations == null) {
            return "";
        }
        String id = locationId.trim();
        for (BasicInfo.LocationInfo loc : locations) {
            if (loc != null && id.equals(loc.locationId) && !TextUtils.isEmpty(loc.locationName)) {
                return loc.locationName.trim();
            }
        }
        return "";
    }

    @Nullable
    private static String buildImageName(
            @Nullable String activityId, @Nullable String cardId, String verifyTime) {
        String act = TextUtils.isEmpty(activityId) ? "ACT" : activityId.trim();
        String card = TextUtils.isEmpty(cardId) ? "CARD" : cardId.trim();
        String timePart = verifyTime.replace("-", "").replace(":", "").replace(" ", "");
        return act + "_" + card + "_" + timePart + ".jpg";
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (!TextUtils.isEmpty(v)) {
                return v.trim();
            }
        }
        return "";
    }
}
