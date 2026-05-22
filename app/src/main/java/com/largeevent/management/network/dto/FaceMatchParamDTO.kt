package com.largeevent.management.network.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * 人脸比对参数 DTO（POST /baidu/faceMatch 请求体数组元素）
 */
@Keep
data class FaceMatchParamDTO(
    /** 图片内容（Base64 或 URL） */
    var image: String? = null,

    /** 图片类型：BASE64 或 URL */
    @SerializedName("image_type")
    var imageType: String? = null,

    /** 人脸图片类型，默认 LIVE */
    @SerializedName("face_type")
    var faceType: String? = null,
)
