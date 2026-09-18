package com.largeevent.management.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 设备信息注册请求 DTO（JSON 字段首字母大写，与后端约定一致）
 */
class EquipmentRegisterAo {
    @SerializedName("AccountNumber")
    @JvmField
    var accountNumber: String? = null

    @SerializedName("ActivityId")
    @JvmField
    var activityId: String? = null

    @SerializedName("CarNumber")
    @JvmField
    var carNumber: String? = null

    @SerializedName("CreateTime")
    @JvmField
    var createTime: String? = null

    @SerializedName("DataState")
    @JvmField
    var dataState: Int = 0

    @SerializedName("Description")
    @JvmField
    var description: String? = null

    @SerializedName("EqpCode")
    @JvmField
    var eqpCode: String? = null

    @SerializedName("EqpID")
    @JvmField
    var eqpID: String? = null

    @SerializedName("EqpIP")
    @JvmField
    var eqpIP: String? = null

    @SerializedName("EqpModel")
    @JvmField
    var eqpModel: String? = null

    @SerializedName("EqpName")
    @JvmField
    var eqpName: String? = null

    @SerializedName("EqpState")
    @JvmField
    var eqpState: Int = 0

    @SerializedName("EqpType")
    @JvmField
    var eqpType: String? = null

    @SerializedName("LocationID")
    @JvmField
    var locationID: String? = null

    @SerializedName("ZoneLocationID")
    @JvmField
    var zoneLocationID: String? = null

    constructor()

    constructor(
        activityId: String?,
        createTime: String?,
        eqpID: String?,
        locationID: String?,
        zoneLocationID: String?
    ) {
        this.activityId = activityId
        this.createTime = createTime
        this.eqpID = eqpID
        this.locationID = locationID
        this.zoneLocationID = zoneLocationID
    }
}
