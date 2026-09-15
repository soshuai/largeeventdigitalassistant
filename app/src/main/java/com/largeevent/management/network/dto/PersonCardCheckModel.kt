package com.largeevent.management.network.dto

/**
 * 人证核验记录接收类（分页 records 单条）。
 *
 * 接口：`POST androidNew/api/selectPersonCheckRecord`
 * 位于 [ApiResponse.data] → [PageResult.records] 的列表元素。
 */
class PersonCardCheckModel {
    /** 核验记录 ID */
    @JvmField
    var entryId: String = ""

    /** 模块类型 */
    @JvmField
    var moduleType: Int = 0

    /** 核验状态 */
    @JvmField
    var verifyStatus: Int = 0

    /** 核验时间 */
    @JvmField
    var verifyTime: String = ""

    /** 进出方向：0 进 / 1 出 等 */
    @JvmField
    var direction: Int = 0

    /** 设备 ID */
    @JvmField
    var equipmentId: String = ""

    /** 设备名称 */
    @JvmField
    var equipmentName: String = ""

    /** 位置 ID */
    @JvmField
    var locationId: String = ""

    /** 位置名称 */
    @JvmField
    var locationName: String = ""

    /** 分区 ID */
    @JvmField
    var zoneLocationId: String = ""

    /** 分区名称 */
    @JvmField
    var zoneLocationName: String = ""

    /** 姓名 */
    @JvmField
    var name: String = ""

    /** 卡号 */
    @JvmField
    var cardNumber: String = ""

    /** 证件号（列表展示优先） */
    @JvmField
    var idNumber: String = ""

    /** 证件/证卡 ID */
    @JvmField
    var cardId: String = ""

    /** 是否已检测（防疫） */
    @JvmField
    var isDetectedStatus: String = ""

    /** 密接状态 */
    @JvmField
    var closeContactStatus: String = ""

    /** 阳性状态 */
    @JvmField
    var positiveStatus: String = ""

    /** 活动 ID */
    @JvmField
    var activityId: String = ""

    /** 现场照片文件名 */
    @JvmField
    var imageName: String = ""

    /** 现场照 URL / 备注图 */
    @JvmField
    var remark: String = ""
}
