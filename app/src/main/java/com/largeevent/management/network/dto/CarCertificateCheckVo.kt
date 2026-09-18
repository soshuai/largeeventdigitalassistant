package com.largeevent.management.network.dto

/**
 * 车证核验记录接收类（分页 records 单条）。
 *
 * 接口：`POST androidNew/api/selectCarCertCheckRecord`
 * 字段名、类型与 [PersonCardCheckVo] 一致。
 */
class CarCertificateCheckVo {
    @JvmField
    var entryId: String = ""

    @JvmField
    var moduleType: Int = 0

    @JvmField
    var verifyStatus: Int = 0

    @JvmField
    var verifyTime: String = ""

    /** 进出方向：0 进 / 1 出 */
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

    /** 车牌（对应人证姓名） */
    @JvmField
    var name: String = ""

    /** 芯片号（对应人证卡号） */
    @JvmField
    var cardNumber: String = ""

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
    var remark: String = ""
}
