package com.largeevent.management.network.dto

/**
 * 设备信息注册请求 DTO
 */
class EquipmentRegisterDTO {
    /** 本机人证数量（可选）  */
    @JvmField
    var accountNumber: String? = null

    /** 活动 ID（必填）  */
    @JvmField
    var activityId: String? = null

    /** 本机车证数量（可选）  */
    @JvmField
    var carNumber: String? = null

    /** 创建时间（必填，格式：yyyy-MM-dd HH:mm:ss）  */
    @JvmField
    var createTime: String? = null

    /** 数据状态（可选，0 正常，1 异常，2 离线）  */
    @JvmField
    var dataState: Int = 0

    /** 设备描述（可选）  */
    @JvmField
    var description: String? = null

    /** 设备编号（可选）  */
    @JvmField
    var eqpCode: String? = null

    /** 设备 ID（必填）  */
    @JvmField
    var eqpID: String? = null

    /** 设备 IP 地址（可选）  */
    @JvmField
    var eqpIP: String? = null

    /** 设备型号（可选）  */
    @JvmField
    var eqpModel: String? = null

    /** 设备名称（可选）  */
    @JvmField
    var eqpName: String? = null

    /** 设备状态（可选，0 正常，1 异常，2 离线）  */
    @JvmField
    var eqpState: Int = 0

    /** 设备类型（可选，人证查验设备/车证查验设备/手持式查验设备）  */
    @JvmField
    var eqpType: String? = null

    /** 场馆型位置 ID（必填）  */
    @JvmField
    var locationID: String? = null

    /** 分区型位置 ID（必填）  */
    @JvmField
    var zoneLocationID: String? = null

    constructor()

    constructor(activityId: String?, createTime: String?, eqpID: String?, locationID: String?, zoneLocationID: String?) {
        this.activityId = activityId
        this.createTime = createTime
        this.eqpID = eqpID
        this.locationID = locationID
        this.zoneLocationID = zoneLocationID
    }
}
