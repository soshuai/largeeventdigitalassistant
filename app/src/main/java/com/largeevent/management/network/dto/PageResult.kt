package com.largeevent.management.network.dto

/**
 * 分页结果
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
