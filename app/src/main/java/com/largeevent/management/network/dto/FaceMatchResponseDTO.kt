package com.largeevent.management.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 人脸比对响应 DTO（百度 API 原始返回格式）
 */
class FaceMatchResponseDTO {
    @JvmField
    @SerializedName("error_code")
    var errorCode: Int = 0

    @JvmField
    @SerializedName("error_msg")
    var errorMsg: String? = null

    @JvmField
    var result: Result? = null

    class Result {
        @JvmField
        var score: Double = 0.0

        @JvmField
        @SerializedName("face_list")
        var faceList: MutableList<FaceInfo?>? = null
    }

    class FaceInfo {
        @JvmField
        @SerializedName("face_token")
        var faceToken: String? = null
    }

    /** 百度 error_code == 0 表示成功 */
    fun isSuccess(): Boolean {
        return errorCode == 0
    }

    fun getScore(): Double {
        return result?.score ?: 0.0
    }

    fun getErrorMessage(): String? {
        return errorMsg
    }
}
