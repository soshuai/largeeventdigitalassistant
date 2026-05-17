package com.largeevent.management.network.dto

/**
 * 车证核验数据上传 DTO
 * 对应接口：POST /android/api/receiveCheckCar
 */
class CarCertificateCheckDTO {
    /** 芯片号（IC物理卡号） */
    @JvmField
    var tagNo1: String? = null

    /** 车牌号码 */
    @JvmField
    var carPlate: String? = null

    /** 核验设备编码 */
    @JvmField
    var deviceCode: String? = null

    /** 核验设备位置 */
    @JvmField
    var location: String? = null

    /** 通行时间（yyyy-MM-dd HH:mm:ss） */
    @JvmField
    var passTime: String? = null

    /** 责任电话（打印在车证上） */
    @JvmField
    var responsibilityPhone: String? = null

    /** 单位 */
    @JvmField
    var organization: String? = null

    /** 检查图片（Base64，或 URL） */
    @JvmField
    var checkImg: String? = null

    /** 验证情况 0-否，1-是 */
    @JvmField
    var checkStatus: Int? = null

    /** 车证类型 */
    @JvmField
    var cardType: String? = null

    /** 进出方向 */
    @JvmField
    var direction: String? = null

    /** 核验结果信息 */
    @JvmField
    var errorMsg: String? = null

    /** 活动ID */
    @JvmField
    var activeId: String? = null

    /** 活动名称 */
    @JvmField
    var activeName: String? = null

    /** 停车区域 */
    @JvmField
    var parkingArea: String? = null

    /** 车证有效开始时间 */
    @JvmField
    var startTime: String? = null

    /** 车证有效结束时间 */
    @JvmField
    var endTime: String? = null
}
