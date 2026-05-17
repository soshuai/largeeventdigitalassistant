package com.largeevent.management.record;

import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;

import java.io.Serializable;

public class VerificationRecord implements Serializable {
    private Long id; // 数据库ID
    public final String type; // 记录类型：人证核验/车证核验
    public final String chipId; // 芯片号
    public final String name; // 姓名
    public final String plateNumber; // 车牌号（车证核验时使用）
    public final String description; // 描述
    public final VerificationResultType resultType; // 核验结果类型
    public final boolean isSuccess; // 是否成功
    public final long timestamp; // 时间戳
    public final boolean isUploaded; // 是否已上传
    public final CertificateInfo certificateInfo; // 证件信息（用于详情页）

    public VerificationRecord(Long id, String type, String chipId, String name, String plateNumber,
                             String description, VerificationResultType resultType, boolean isSuccess,
                             long timestamp, boolean isUploaded, CertificateInfo certificateInfo) {
        this.id = id;
        this.type = type;
        this.chipId = chipId;
        this.name = name;
        this.plateNumber = plateNumber;
        this.description = description;
        this.resultType = resultType;
        this.isSuccess = isSuccess;
        this.timestamp = timestamp;
        this.isUploaded = isUploaded;
        this.certificateInfo = certificateInfo;
    }

    // 从VerificationResult创建记录
    public static VerificationRecord fromResult(String type, String chipId, VerificationResult result) {
        CertificateInfo info = result.certificateInfo;
        String name = info != null ? info.name : "";
        String plateNumber = info != null ? info.cardSerial : "";
        boolean isSuccess = result.type == VerificationResultType.PASS;
        
        return new VerificationRecord(null, type, chipId, name, plateNumber, result.description, result.type, isSuccess,
                System.currentTimeMillis(), false, info);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VerificationResult toVerificationResult() {
        return new VerificationResult(
                resultType,
                isSuccess ? "核验通过" : "核验失败",
                description,
                certificateInfo,
                false,
                null,
                "车证核验".equals(type)
        );
    }
}


