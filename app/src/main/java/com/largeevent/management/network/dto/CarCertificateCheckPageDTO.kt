package com.largeevent.management.network.dto

/**
 * 车证核验记录分页查询 DTO
 */
class CarCertificateCheckPageDTO : PageDto() {
    /** 芯片号（IC 卡号，支持模糊查询） */
    @JvmField
    var tagNo1: String? = null

    /** 车牌号码（支持模糊查询） */
    @JvmField
    var carPlate: String? = null

    /** 验证情况：0-否，1-是 */
    @JvmField
    var checkStatus: Int? = null

    /** 车证类型 */
    @JvmField
    var cardType: String? = null

    /** 开始时间 */
    @JvmField
    var beginTime: String? = null

    /** 结束时间 */
    @JvmField
    var endTime: String? = null
}
