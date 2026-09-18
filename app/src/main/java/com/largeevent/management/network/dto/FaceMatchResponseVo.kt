package com.largeevent.management.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 人脸比对响应接收类（非 ApiResponse 包装）。
 *
 * 接口：`POST baidu/faceMatch`
 * 兼容两种格式：
 * 1. 服务端包装：`{ "code": 200, "msg": "...", "data": { "result": { "score": ... } } }`
 * 2. 百度原始：`{ "error_code": 0, "error_msg": "SUCCESS", "result": { "score": ... } }`
 */
class FaceMatchResponseVo {
    /** 服务端通用 code（200 表示成功） */
    @JvmField
    var code: Int? = null

    @JvmField
    var msg: String? = null

    @JvmField
    var data: DataPayload? = null

    @JvmField
    @SerializedName("error_code")
    var errorCode: Int = 0

    @JvmField
    @SerializedName("error_msg")
    var errorMsg: String? = null

    @JvmField
    var result: Result? = null

    class DataPayload {
        @JvmField
        @SerializedName("error_code")
        var errorCode: Int = 0

        @JvmField
        @SerializedName("error_msg")
        var errorMsg: String? = null

        @JvmField
        var result: Result? = null
    }

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

    /** 接口是否成功 */
    fun isSuccess(): Boolean {
        if (code != null) {
            if (code != 200) {
                return false
            }
            data?.let {
                if (it.errorCode != 0) {
                    return false
                }
            }
            return true
        }
        return errorCode == 0
    }

    fun resolveResult(): Result? {
        data?.result?.let { return it }
        return result
    }

    fun getScore(): Double {
        return resolveResult()?.score ?: 0.0
    }

    fun getErrorMessage(): String? {
        if (!msg.isNullOrBlank()) {
            return msg
        }
        if (!errorMsg.isNullOrBlank()) {
            return errorMsg
        }
        return data?.errorMsg
    }
}
