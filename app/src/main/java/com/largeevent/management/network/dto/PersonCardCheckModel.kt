package com.largeevent.management.network.dto

/**
 * selectPersonCheckRecord 响应 records 单条（字段名、类型与后端一致）
 */
class PersonCardCheckModel {
    @JvmField
    var entryId: String = ""

    @JvmField
    var moduleType: Int = 0

    @JvmField
    var verifyStatus: Int = 0

    @JvmField
    var verifyTime: String = ""

    @JvmField
    var direction: Int = 0

    @JvmField
    var equipmentId: String = ""

    @JvmField
    var equipmentName: String = ""

    @JvmField
    var locationId: String = ""

    @JvmField
    var locationName: String = ""

    @JvmField
    var zoneLocationId: String = ""

    @JvmField
    var zoneLocationName: String = ""

    @JvmField
    var name: String = ""

    @JvmField
    var cardNumber: String = ""

    /** 证件号（列表展示用，优先于 cardNumber） */
    @JvmField
    var idNumber: String = ""

    @JvmField
    var cardId: String = ""

    @JvmField
    var isDetectedStatus: String = ""

    @JvmField
    var closeContactStatus: String = ""

    @JvmField
    var positiveStatus: String = ""

    @JvmField
    var activityId: String = ""

    @JvmField
    var imageName: String = ""

    @JvmField
    /** 现场照 URL */
    var remark: String = ""
}
