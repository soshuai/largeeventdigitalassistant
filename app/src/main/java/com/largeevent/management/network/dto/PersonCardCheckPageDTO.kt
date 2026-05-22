package com.largeevent.management.network.dto

/**
 * POST /selectPersonCheckRecord 请求体（字段名、类型与后端一致）
 */
class PersonCardCheckPageDTO {
    @JvmField
    var current: Int = 1

    @JvmField
    var size: Int = 10

    @JvmField
    var name: String? = null

    @JvmField
    var cardNumber: String? = null

    @JvmField
    var cardId: String? = null

    @JvmField
    var activityId: String? = null

    @JvmField
    var verifyStatus: Int? = null

    @JvmField
    var equipmentName: String? = null

    @JvmField
    var locationName: String? = null

    @JvmField
    var beginTime: String? = null

    @JvmField
    var endTime: String? = null
}
