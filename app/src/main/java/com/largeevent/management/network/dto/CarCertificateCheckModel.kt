package com.largeevent.management.network.dto

import java.util.Date

/**
 * 车证核验记录接收类（分页 records 单条）。
 *
 * 接口：`POST androidNew/api/selectCarCertCheckRecord`
 * 位于 [ApiResponse.data] → [PageResult.records] 的列表元素。
 */
class CarCertificateCheckModel {
    /** 芯片号 / 标签号 */
    @JvmField var tagNo1: String? = null
    /** 车牌号 */
    @JvmField var carPlate: String? = null
    /** 设备编码 */
    @JvmField var deviceCode: String? = null
    /** 核验位置 */
    @JvmField var location: String? = null
    /** 通行时间 */
    @JvmField var passTime: Date? = null
    /** 责任人电话 */
    @JvmField var responsibilityPhone: String? = null
    /** 所属单位/组织 */
    @JvmField var organization: String? = null
    /** 核验图片 */
    @JvmField var checkImg: String? = null
    /** 证件类型 */
    @JvmField var cardType: String? = null
    /** 车牌颜色 */
    @JvmField var licensePlateColor: String? = null
    /** 有效期开始 */
    @JvmField var startTime: String? = null
    /** 有效期结束 */
    @JvmField var endTime: String? = null
    /** 通行权限描述 */
    @JvmField var accessauthority: String? = null
    /** 活动/赛事状态 */
    @JvmField var event_status: String? = null
    /** 核验结果状态 */
    @JvmField var checkStatus: Int? = null
    /** 失败原因 */
    @JvmField var errorMsg: String? = null
    /** 进出方向 */
    @JvmField var direction: String? = null
    /** 记录创建时间 */
    @JvmField var createTime: Date? = null
}
