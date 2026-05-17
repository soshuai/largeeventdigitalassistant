package com.largeevent.management.network.dto

/**
 * 分页参数基类
 */
open class PageDto {
    /** 当前页码，默认 1 */
    @JvmField
    var current: Long = 1

    /** 分页大小，默认 10 */
    @JvmField
    var size: Long = 10
}
