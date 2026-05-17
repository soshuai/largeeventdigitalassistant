package com.largeevent.management.network.dto

import androidx.annotation.Keep

/**
 * 人脸比对参数 DTO
 */
@Keep
data class FaceMatchParamDTO (
    /** 图片内容（Base64 或 URL）  */
    var image: String? = null,

    /** 图片类型，BASE64或URL  */
    var imageType: String? = null,

    /** 人脸图片类型：默认LIVE  */
    var faceType: String? = null
)
