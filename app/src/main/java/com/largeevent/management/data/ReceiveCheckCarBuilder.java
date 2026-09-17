package com.largeevent.management.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;
import com.largeevent.management.network.dto.CarCertificateDTO;
import com.largeevent.management.network.dto.ReceiveCheckCarDTO;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 按 receiveCheckPerson 同结构组装 receiveCheckCar 上传体。
 */
public final class ReceiveCheckCarBuilder {

    private static final String EPIDEMIC_DISABLED = "2";

    private ReceiveCheckCarBuilder() {
    }

    public static ReceiveCheckCarDTO build(
            Context context,
            VerificationResult result,
            @Nullable CarCertificateDTO car,
            @Nullable String chipId,
            @Nullable BasicInfo basicInfo) {
        ReceiveCheckCarDTO dto = new ReceiveCheckCarDTO();
        String activeId = AppPreferences.getLastActiveId(context);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String now = sdf.format(new Date());

        dto.entryId = UUID.randomUUID().toString().replace("-", "");
        dto.verifyTime = now;
        dto.createTime = now;
        dto.verifyStatus = mapVerifyStatus(result);
        dto.direction = result != null && result.type == VerificationResultType.PASS ? 0 : 1;

        dto.equipmentId = AppPreferences.getDeviceCode(context);
        dto.equipmentName = android.os.Build.MODEL;

        dto.locationId = AppPreferences.getSelectedLocationId(
                context, activeId, ModuleType.VEHICLE);
        dto.zoneLocationId = AppPreferences.getSelectedZoneId(
                context, activeId, ModuleType.VEHICLE);
        dto.locationName = resolveLocationName(basicInfo, dto.locationId);
        dto.zoneLocationName = resolveLocationName(basicInfo, dto.zoneLocationId);

        if (!TextUtils.isEmpty(activeId)) {
            dto.activityId = activeId.trim();
        }

        dto.name = resolveName(car);
        dto.cardId = firstNonEmpty(
                CarCertificateParser.resolveCardId(car),
                car != null ? car.id : null,
                chipId);
        dto.cardNumber = firstNonEmpty(
                car != null ? car.tagNo1 : null,
                chipId);

        dto.isDetectedStatus = EPIDEMIC_DISABLED;
        dto.closeContactStatus = EPIDEMIC_DISABLED;
        dto.positiveStatus = EPIDEMIC_DISABLED;

        if (result != null && !TextUtils.isEmpty(result.description)) {
            dto.remark = result.description.trim();
        } else if (result != null && !TextUtils.isEmpty(result.title)) {
            dto.remark = result.title.trim();
        }

        return dto;
    }

    private static int mapVerifyStatus(@Nullable VerificationResult result) {
        if (result == null) {
            return VerificationResultType.NOT_DETECTED.code;
        }
        int code = result.type != null
                ? result.type.code
                : VerificationResultType.NOT_DETECTED.code;
        if (code == VerificationResultType.UNBOUND.code) {
            return VerificationResultType.FACE_MISMATCH.code;
        }
        if (code == VerificationResultType.BIND_REQUIRED.code) {
            return VerificationResultType.INVALID_CERT.code;
        }
        return code;
    }

    private static String resolveName(@Nullable CarCertificateDTO car) {
        String plate = CarCertificateParser.resolvePlate(car);
        if (!TextUtils.isEmpty(plate)) {
            return plate.trim();
        }
        String office = CarCertificateParser.resolveOffice(car);
        if (!TextUtils.isEmpty(office)) {
            return office.trim();
        }
        return "";
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
}
