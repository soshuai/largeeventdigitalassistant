package com.largeevent.management.network.dto

/**
 * 更新人员照片请求 DTO
 */
class UpdatePhotoDTO {
    /** 活动编码  */
//    var activeCode: String? = null

    /** 证件号码，可为身份证号、芯片号、证件号  */
    var certNumber: String? = null

    /** 需要更新的照片（Base64 格式，带 data:image/jpeg;base64,前缀）  */
    var base64Img: String? = null

    constructor()

    constructor(certNumber: String?, base64Img: String?) {
        this.certNumber = certNumber
        this.base64Img = base64Img
    }
}
