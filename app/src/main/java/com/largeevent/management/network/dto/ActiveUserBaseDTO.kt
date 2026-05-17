package com.largeevent.management.network.dto

/**
 * 活动人员基础数据 DTO
 * 对应接口：GET /android/api/getActiveUser
 */
class ActiveUserBaseDTO { // Getters
    // Setters
    /** 用户id  */
    @JvmField
    var userId: String? = null

    /** 证件ID  */
    @JvmField
    var certId: String? = null

    /** 活动id  */
    @JvmField
    var activeId: String? = null

    /** 注册号  */
    @JvmField
    var registerNumber: String? = null

    /** 手机号  */
    @JvmField
    var phoneNumber: String? = null

    /** 归口部门编码  */
    @JvmField
    var organizationCode: String? = null

    /** 归口部门名称  */
    @JvmField
    var organization: String? = null

    /** 证件类型  */
    @JvmField
    var idType: String? = null

    /** 证件号码  */
    @JvmField
    var idNumber: String? = null

    @JvmField
    var number: String? = null

    /** 中文姓名  */
    @JvmField
    var chineseName: String? = null

    /** 所属单位编码  */
    @JvmField
    var affiliatedUnitCode: String? = null

    /** 所属单位名称  */
    @JvmField
    var affiliatedUnit: String? = null

    /** 岗位  */
    @JvmField
    var profile: String? = null

    /** 出生日期，格式 yyyy-MM-dd  */
    @JvmField
    var birth: String? = null

    /** 性别  */
    @JvmField
    var gender: String? = null

    /** 电子照片（URL 或 base64）  */
    @JvmField
    var photo: String? = null

    /** 是否已实名认证（0 未认证，1 已认证）  */
    @JvmField
    var realStatus: Int = 0

    /** 活动单位编码  */
    @JvmField
    var activeUnitCode: String? = null

    /** 活动单位名称  */
    @JvmField
    var activeUnit: String? = null

    /** 活动岗位  */
    @JvmField
    var activeProfile: String? = null

    /** 通行代码  */
    @JvmField
    var passRuleCode: String? = null

    /** 芯片号（主）  */
    @JvmField
    var tagNo1: String? = null

    /** 芯片号2（备）  */
    @JvmField
    var tagNo2: String? = null

    /** 证件类型  */
    @JvmField
    var subAppTypeCode: String? = null

    /** 01：实名证件； 02： 现场绑定实名证件（未绑定不允许通行）；03：非实名证件（无需人证合一，比如VIP证件等）  */
    @JvmField
    var mainAppTypeCode: String? = null

    /** 是否挂失（0-正常，1-挂失）  */
    @JvmField
    var cartStatus: String ? = null

    /** 是否注销（0-正常，1-注销）  */
    @JvmField
    var cancelCard: String? = null

    /** 0-否，1-是  */
    @JvmField
    var ctidStatus: String ? = null

    /** 失败原因  */
    @JvmField
    var ctidErrorMsg: String? = null

    /** 有效期开始时间  */
    @JvmField
    var validBegin: String? = null

    /** 有效期结束时间  */
    @JvmField
    var validEnd: String? = null

    /** 是否删除（0-正常，1-删除）  */
    @JvmField
    var deleted: String? = null

    /** 绑定状态(0-已绑定 1-未绑定)  */
    @JvmField
    var bindStatus: String? = null

    // ==================== 文档原有字段 ====================

    /** 人脸模型  */
    @JvmField
    var faceModel: String? = null

    /** PUF  */
    @JvmField
    var puf: String? = null

    /** 验证头像 Base64  */
    @JvmField
    var verifyPhoto: String? = null

    /** 背审状态（0 不通过，1 通过，2 无效，3 审核中，4 待审核）  */
    @JvmField
    var bsStatus: String? = null

    /** 黑名单标记（0 正常，1 黑名单）  */
    @JvmField
    var blackSign: Int = 0

    /** 证件状态（0 未打印，1 打印中，2 已打印，3 已发卡，4 已核验，5 已激活，6 已注销）  */
    @JvmField
    var eventStatus: Int = 0

    /** 场馆权限  */
    @JvmField
    var venuePrivileges: String? = null

    /** 区域权限  */
    @JvmField
    var areaPrivileges: String? = null

    /** 分区权限  */
    @JvmField
    var zonePrivileges: String? = null

    /** 座位权限  */
    @JvmField
    var standPrivileges: String? = null

    /** 附加权限  */
    @JvmField
    var additionalPrivileges: String? = null

    /** 日通行证生效日期  */
    @JvmField
    var effectiveDateOfDayPass: String? = null
}
