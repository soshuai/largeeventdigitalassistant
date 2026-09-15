package com.largeevent.management.network.dto

/**
 * 通用分页结果接收类。
 *
 * 用于 `selectPersonCheckRecord` / `selectCarCertCheckRecord` 等分页接口，
 * 位于 [ApiResponse.data]；列表在 [records]。
 *
 * @param T 单条记录类型，如 [PersonCardCheckModel]、[CarCertificateCheckModel]
 */
class PageResult<T> {
    /** 数据列表 */
    @JvmField
    var records: List<T>? = null

    /** 总数 */
    @JvmField
    var total: Long = 0

    /** 分页大小 */
    @JvmField
    var size: Long = 10

    /** 当前页码 */
    @JvmField
    var current: Long = 1

    /** 总页数 */
    @JvmField
    var pages: Long = 0
}
