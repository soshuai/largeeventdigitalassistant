package com.largeevent.management.network.dto

/**
 * 绑定人证数据请求DTO
 * 接口: POST /android/api/bandCert
 */
class BindCertDTO { // Getters and Setters
    /** 活动编码  */
    @JvmField
    var activeCode: String? = null

    /** 证件类型：1=实名证，2=现场绑定实名证，3=非实名临时证  */
    @JvmField
    var certType: String? = null

    /** 证件号码(小程序时必填)  */
    @JvmField
    var certNumber: String? = null

    /** 身份证号码（实名认证使用）  */
    @JvmField
    var idNumber: String? = null

    /** 人员姓名  */
    @JvmField
    var personName: String? = null

    /** 照片（Base64 格式）  */
    @JvmField
    var base64Img: String? = null

    /** 芯片号（手持终端、闸机使用时必填）  */
    @JvmField
    var tagNo: String? = null

    /** 小程序 OpenId（小程序时必填）  */
    var openId: String? = null
}
