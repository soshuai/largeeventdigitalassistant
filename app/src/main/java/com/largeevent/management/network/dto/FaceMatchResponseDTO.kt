package com.largeevent.management.network.dto

import com.google.gson.annotations.*

/**
 * 人脸比对响应 DTO
 */
class FaceMatchResponseDTO {
    @JvmField
    var code: Int = 0

    @JvmField
    var msg: String? = null

    @JvmField
    var message: String? = null

    @JvmField
    var data: Data? = null

    class Data {
        @JvmField
        var result: Result? = null

        @JvmField
        var logId: Long = 0

        @JvmField
        var errorMsg: String? = null

        @JvmField
        var cached: Int = 0

        @JvmField
        var errorCode: Int = 0

        @JvmField
        var timestamp: Long = 0
    }

    class Result {
        @JvmField
        var score: Double = 0.0

        @JvmField
        var faceList: MutableList<FaceInfo?>? = null
    }

    class FaceInfo {
        @JvmField
        var faceToken: String? = null
    }

    // 便捷方法，判断接口调用是否成功
    fun isSuccess(): Boolean {
        return code == 200
    }

    // 便捷方法，获取score分数
    fun getScore(): Double {
        return data?.result?.score ?: 0.0
    }

    // 便捷方法，获取错误信息
    fun getErrorMessage(): String? {
        return if (code != 200) {
            msg ?: message
        } else {
            data?.errorMsg
        }
    }
}
