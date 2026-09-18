package com.largeevent.management.data;

import android.text.TextUtils;

import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.network.dto.CarCertificateVo;

import org.json.JSONObject;

import androidx.annotation.Nullable;

/**
 * 车证数据解析器
 */
public class CarCertificateParser {

    /**
     * 解析单个车证数据为 CarCertificateVo
     */
    public static CarCertificateVo parseCarCertificate(JSONObject json) {
        if (json == null) {
            return null;
        }
        
        CarCertificateVo dto = new CarCertificateVo();
        dto.id = json.optString("id");
        dto.createBy = json.optString("createBy");
        dto.createTime = json.optString("createTime");
        dto.updateBy = json.optString("updateBy");
        dto.updateTime = json.optString("updateTime");
        dto.isDeleted = json.optInt("isDeleted", 0);
        dto.remark = json.optString("remark");
        dto.number = json.optString("number");
        dto.organization = json.optString("organization");
        dto.tagNo1 = json.optString("tagNo1");
        dto.carUsage = json.optString("carUsage");
        dto.carPlate = json.optString("carPlate");
        dto.parkingArea = json.optString("parkingArea");
        dto.responsibilityPhone = json.optString("responsibilityPhone");
        dto.parkingCode = json.optString("parkingCode");
        dto.venueCodeChildren = json.optString("venueCodeChildren");
        dto.cardType = json.optString("cardType");
        dto.licensePlateColor = json.optString("licensePlateColor");
        dto.startTime = json.optString("startTime");
        dto.endTime = json.optString("endTime");
        dto.accessAuthority = json.optString("accessAuthority");
        dto.eventStatus = json.optString("eventStatus");
        dto.cardId = json.optString("cardId");
        dto.applicantOffice = json.optString("applicantOffice");
        dto.licensePlateNum = json.optString("licensePlateNum");
        
        return dto;
    }

    @Nullable
    public static String resolveOffice(@Nullable CarCertificateVo dto) {
        if (dto == null) {
            return null;
        }
        return firstNonEmpty(dto.applicantOffice, dto.organization);
    }

    @Nullable
    public static String resolvePlate(@Nullable CarCertificateVo dto) {
        if (dto == null) {
            return null;
        }
        return firstNonEmpty(dto.licensePlateNum, dto.carPlate);
    }

    @Nullable
    public static String resolveCardId(@Nullable CarCertificateVo dto) {
        if (dto == null) {
            return null;
        }
        return firstNonEmpty(dto.cardId, dto.number);
    }

    @Nullable
    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 从 CarCertificateVo 构建 CertificateInfo
     */
    public static CertificateInfo buildCertificateInfo(CarCertificateVo dto, String fallbackChipId) {
        if (dto == null) {
            return null;
        }
        
        String chipId = !TextUtils.isEmpty(dto.tagNo1) ? dto.tagNo1 : fallbackChipId;
        if (TextUtils.isEmpty(chipId)) {
            return null;
        }
        
        CertificateInfo.Builder builder = new CertificateInfo.Builder()
                .setName(resolveOffice(dto))
                .setDocumentType("车证")
                .setCertId(resolveCardId(dto))
                .setNumber(resolveCardId(dto))
                .setChipId(chipId)
                .setCardSerial(resolvePlate(dto))
                .setValidFrom(!TextUtils.isEmpty(dto.startTime) ? dto.startTime : "")
                .setValidTo(!TextUtils.isEmpty(dto.endTime) ? dto.endTime : "")
                .setNeedBinding(false)
                .setBound(!TextUtils.isEmpty(resolvePlate(dto)))
                .setRealNameRequired(false)
                .setVenuePrivileges(dto.venueCodeChildren)
                .setAreaPrivileges(dto.parkingCode);
        
        return builder.build();
    }
}
