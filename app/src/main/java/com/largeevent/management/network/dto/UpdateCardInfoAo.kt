package com.largeevent.management.network.dto

/**
 * 更新（绑定）证件信息请求 DTO
 */
class UpdateCardInfoAo {
    /** 主键 ID（可选）  */
    @JvmField
    var accId: String? = null

    /** 芯片号（必填）  */
    @JvmField
    var chipId: String? = null

    /** 活动代码（必填）  */
    @JvmField
    var eventCode: String? = null

    /** 固定密码（可选）  */
    @JvmField
    var password: String? = null

    /** 活动 ID（可选）  */
    @JvmField
    var activityId: String? = null

    /** 姓名（可选）  */
    @JvmField
    var name: String? = null

    /** 单位名称（可选）  */
    @JvmField
    var unitCodeDesc: String? = null

    /** 照片 Base64 字符串（可选）  */
    @JvmField
    var verifyPhoto: String? = null

    /** 有效期开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）  */
    @JvmField
    var validBegin: String? = null

    /** 有效期结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）  */
    @JvmField
    var validEnd: String? = null

    /** 身份证号（必填）  */
    @JvmField
    var identityDocumentNumber: String? = null

    /** 注册号（可选）  */
    @JvmField
    var registrationNumber: String? = null

    constructor()

    constructor(chipId: String?, eventCode: String?, identityDocumentNumber: String?) {
        this.chipId = chipId
        this.eventCode = eventCode
        this.identityDocumentNumber = identityDocumentNumber
    }
}
