package com.largeevent.management.data;

import android.text.TextUtils;

import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.network.dto.CarCertificateDTO;

import org.json.JSONObject;

/**
 * 车证数据解析器
 */
public class CarCertificateParser {

    /**
     * 解析单个车证数据为 CarCertificateDTO
     */
    public static CarCertificateDTO parseCarCertificate(JSONObject json) {
        if (json == null) {
            return null;
        }
        
        CarCertificateDTO dto = new CarCertificateDTO();
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
        dto.area = json.optString("area");
        dto.cardType = json.optString("cardType");
        dto.licensePlateColor = json.optString("licensePlateColor");
        dto.startTime = json.optString("startTime");
        dto.endTime = json.optString("endTime");
        dto.accessAuthority = json.optString("accessAuthority");
        dto.eventStatus = json.optString("eventStatus");
        
        return dto;
    }

    /**
     * 从 CarCertificateDTO 构建 CertificateInfo
     */
    public static CertificateInfo buildCertificateInfo(CarCertificateDTO dto, String fallbackChipId) {
        if (dto == null) {
            return null;
        }
        
        String chipId = !TextUtils.isEmpty(dto.tagNo1) ? dto.tagNo1 : fallbackChipId;
        if (TextUtils.isEmpty(chipId)) {
            return null;
        }
        
        CertificateInfo.Builder builder = new CertificateInfo.Builder()
                .setName(dto.organization)  // 单位名称
                .setDocumentType("车证")
                .setNumber(dto.number)
                .setChipId(chipId)
                .setCardSerial(dto.carPlate)  // 编号
                .setValidFrom(!TextUtils.isEmpty(dto.startTime) ? dto.startTime : "")
                .setValidTo(!TextUtils.isEmpty(dto.endTime) ? dto.endTime : "")
                .setNeedBinding(false)  // 车证不需要绑定
                .setBound(true)
                .setRealNameRequired(false);  // 车证不需要人脸识别
        
        // 添加权限（优先使用 accessAuthority，其次使用 area）
        String authority = dto.accessAuthority;
        if (!TextUtils.isEmpty(authority)) {
            // 使用出入权限字段
            builder.addPermission(authority);
        } else {
            // 降级使用 area 字段
            String area = dto.area;
            if (!TextUtils.isEmpty(area)) {
                builder.addPermission(area);
            } else {
                builder.addPermission("ALL");
            }
        }
        
        return builder.build();
    }
}
