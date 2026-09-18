package com.largeevent.management.network.dto

/**
 * POST /receiveCheckPerson 请求体（字段名、类型与后端一致）
 */
class ReceiveCheckPersonAo {
    @JvmField
    var entryId: String = ""

    @JvmField
    var verifyStatus: Int = 0

    @JvmField
    var verifyTime: String = ""

    /** 0-进入，1-离开 */
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

    @JvmField
    var cardId: String = ""

    @JvmField
    var createTime: String = ""

    @JvmField
    var isDetectedStatus: String = "2"

    @JvmField
    var closeContactStatus: String = "2"

    @JvmField
    var positiveStatus: String = "2"

    @JvmField
    var activityId: String = ""

    @JvmField
    var imageName: String = ""

    @JvmField
    var remark: String = ""
}
