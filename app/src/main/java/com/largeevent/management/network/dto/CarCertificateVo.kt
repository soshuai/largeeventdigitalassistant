package com.largeevent.management.network.dto

import java.io.*
import java.util.Date

/**
 * 车证详情接收类。
 *
 * 接口：
 * - `GET androidNew/api/getActiveCarCert` — [ApiResponse.data] 列表元素
 * - `POST androidNew/api/updateCarCert` — 请求体（响应为原始 ResponseBody）
 */
class CarCertificateVo : Serializable { // Getters
    // Setters
    /** ID  */
    @JvmField
    var id: String? = null

    /** 创建人  */
    @JvmField
    var createBy: String? = null

    /** 创建时间  */
    @JvmField
    var createTime: String? = null

    /** 更新人  */
    @JvmField
    var updateBy: String? = null

    /** 更新时间  */
    @JvmField
    var updateTime: String? = null

    /** 是否删除  */
    @JvmField
    var isDeleted: Int = 0

    /** 备注  */
    @JvmField
    var remark: String? = null

    /** 编号（导入序号或业务编号）  */
    @JvmField
    var number: String? = null

    /** 单位名称  */
    @JvmField
    var organization: String? = null

    /** 芯片号（IC 物理卡号）  */
    @JvmField
    var tagNo1: String? = null

    /** 用途（如"赛事用车""通勤"等）  */
    @JvmField
    var carUsage: String? = null

    /** 车牌号码  */
    @JvmField
    var carPlate: String? = null

    /** 停车区域（单选）  */
    @JvmField
    var parkingArea: String? = null

    /** 责任电话（打印在车证上）  */
    @JvmField
    var responsibilityPhone: String? = null

    /** 停车通行码（对应设备 park / parkVal）  */
    @JvmField
    var parkingCode: String? = null

    /** 场馆权限（对应设备 venue）  */
    @JvmField
    var venueCodeChildren: String? = null

    /** 证件类型 */
    @JvmField
    var cardType: String? = null

    /** 车牌颜色 */
    @JvmField
    var licensePlateColor: String? = null

    /** 有效期开始 */
    @JvmField
    var startTime: String? = null

    /** 有效期结束 */
    @JvmField
    var endTime: String? = null

    /** 通行权限描述 */
    @JvmField
    var accessAuthority: String? = null

    /** 活动/赛事状态 */
    @JvmField
    var eventStatus: String? = null

    /**
     * 是否挂失(0-正常，1-挂失)
     */
    @JvmField
    var isLost: String? = null

    /** 活动 ID */
    @JvmField
    var activeId: String? = null

    /** 编号  */
    @JvmField
    var cardId: String? = null

    /** 单位  */
    @JvmField
    var applicantOffice: String? = null

    // ==================== 新增字段 ====================

    /** 车牌号（同 carPlate，保持兼容）  */
    @JvmField
    var licensePlateNum: String? = null

    /** 车辆类型 ID  */
    @JvmField
    var carTypeId: String? = null

    /** 车辆类型  */
    @JvmField
    var carType: String? = null

    /** 疫情标志（0 正常期间，1 疫情期间）  */
    @JvmField
    var plagueFlag: Int = 0

    /** 车辆标志（0 正常可用，1 疫情可用，-1 疫情不可用）  */
    @JvmField
    var carFlag: Int = 0

    /** 数据来源（0 导入，1 其他）  */
    @JvmField
    var dataSource: Int = 0
}
