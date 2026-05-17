package com.largeevent.management.network.dto

/**
 * 人证核验记录分页查询 DTO
 */
class PersonCardCheckPageDTO : PageDto() {
    /** 姓名（模糊查询） */
    @JvmField
    var personName: String? = null

    /** 身份证号（模糊查询） */
    @JvmField
    var idCard: String? = null

    /** 芯片号（模糊查询） */
    @JvmField
    var tagNo: String? = null

    /** 证件编号（模糊查询） */
    @JvmField
    var certNumber: String? = null

    /** 验证情况：0-否，1-是 */
    @JvmField
    var checkStatus: Int? = null

    /** 开始时间 */
    @JvmField
    var beginTime: String? = null

    /** 结束时间 */
    @JvmField
    var endTime: String? = null
}
