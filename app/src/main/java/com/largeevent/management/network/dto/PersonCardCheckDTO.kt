package com.largeevent.management.network.dto

/**
 * 人证核验数据上传 DTO
 */
class PersonCardCheckDTO { // Getters and Setters
    @JvmField
    var idCard: String? = null // 身份证号
    @JvmField
    var activeCode: String? = null // 活动代码
    @JvmField
    var activeId: String? = null // 活动ID
    @JvmField
    var userId: String? = null // 用户ID
    @JvmField
    var personType: String? = null // 人员类型
    @JvmField
    var personName: String? = null // 姓名
    @JvmField
    var sex: String? = null // 性别
    @JvmField
    var unitName: String? = null // 单位名称
    @JvmField
    var phone: String? = null // 电话
    @JvmField
    var personPhoto: String? = null // 人员照片URL
    @JvmField
    var subAppTypeCode: String? = null // 子应用类型代码
    @JvmField
    var tagNo: String? = null // 芯片号
    @JvmField
    var session: String? = null // 场次
    @JvmField
    var deviceName: String? = null // 设备名称
    @JvmField
    var location: String? = null // 位置
    @JvmField
    var positionId: String? = null // 位置
    @JvmField
    var passRuleCode: String? = null // 通行规则代码
    @JvmField
    var passPositionCode: String? = null // 通行位置代码
    @JvmField
    var certNumber: String? = null // 卡号
    @JvmField
    var passTime: String? = null // 通行时间
    @JvmField
    var direction: String? = null // 方向（进入/离开）
    @JvmField
    var errorMsg: String? = null // 核验结果信息
    @JvmField
    var activeName: String? = null // 活动名称
    @JvmField
    var startTime: String? = null // 证件有效期开始时间
    @JvmField
    var endTime: String? = null // 证件有效期结束时间
    @JvmField
    var checkStatus: Integer? = null //是否验证通过0否1是
    @JvmField
    var checkImg: String? = null //验证记录照片,base64格式
    @JvmField
    var verifyStatus: Int? = null //验证结果状态码（1-未识读出证件, 2-无效证件, 3-限制通行, 4-证件已注销, 5-证件未激活, 6-无权通行, 8-通过, 9-请检查证件, 10-未实名绑定）
    @JvmField
    var verifyDirection: String? = null //验证方向（进入/拒绝）

    // ==================== 新增防疫相关字段 ====================
    @JvmField
    var zoneLocationName: String? = null // 分区型位置名称

    @JvmField
    var isDetectedStatus: String? = null // 核酸检测状态（0 应测已测，1 应测未测，2 未启用）

    @JvmField
    var closeContactStatus: String? = null // 密接状态（0 非密接人员，1 密接人员，2 未启用）

    @JvmField
    var positiveStatus: String? = null // 阳性状态（0 核酸阴性，1 核酸阳性，2 未启用）
}
