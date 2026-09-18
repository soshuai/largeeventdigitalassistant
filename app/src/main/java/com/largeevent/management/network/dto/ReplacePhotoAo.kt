package com.largeevent.management.network.dto

/**
 * 证件照片替换请求 DTO
 */
class ReplacePhotoAo {
    /** 证件 ID  */
    @JvmField
    var accId: String? = null

    /** 活动代码  */
    @JvmField
    var eventCode: String? = null

    /** 照片 Base64 字符串  */
    @JvmField
    var verifyPhoto: String? = null

    /** 活动 ID（可选）  */
    @JvmField
    var activityId: String? = null

    constructor()

    constructor(accId: String?, eventCode: String?, verifyPhoto: String?) {
        this.accId = accId
        this.eventCode = eventCode
        this.verifyPhoto = verifyPhoto
    }
}
