package com.largeevent.management.network.dto

/**
 * 活动人员/证件接收类。
 *
 * 接口：
 * - `GET androidNew/api/getActiveUser`
 * - `GET androidNew/api/getActiveUserByIdNumber`
 *
 * 位于 [ApiResponse.data] 的列表元素。
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

    /** 证件类型（旧字段，兼容）  */
    @JvmField
    var idType: String? = null

    /** 证件号码（旧字段，兼容）  */
    @JvmField
    var idNumber: String? = null

    /** 身份证类型（getActiveUser 标准字段）  */
    @JvmField
    var identityDocumentType: String? = null

    /** 身份证号码（getActiveUser 标准字段）  */
    @JvmField
    var identityDocumentNumber: String? = null

    /** 注册编号  */
    @JvmField
    var registrationNumber: String? = null

    /** 通行证类型 MP/TP/VP  */
    @JvmField
    var passType: String? = null

    /** 中文姓  */
    @JvmField
    var familyNameChinese: String? = null

    /** 中文名  */
    @JvmField
    var givenNameChinese: String? = null

    /** 英文姓  */
    @JvmField
    var familyNameInEnglish: String? = null

    /** 英文名  */
    @JvmField
    var givenNameInEnglish: String? = null

    /** 首选中文姓 */
    @JvmField
    var preferredChineseFamilyName: String? = null

    /** 首选中文名 */
    @JvmField
    var preferredChineseGivenName: String? = null

    /** 首选英文姓 */
    @JvmField
    var preferredFamilyName: String? = null

    /** 首选英文名 */
    @JvmField
    var preferredGivenName: String? = null

    /** 芯片号（接口字段 chipid / chipId）  */
    @JvmField
    @com.google.gson.annotations.SerializedName(value = "chipid", alternate = ["chipId"])
    var chipid: String? = null

    /** 活动 ID（接口字段 activityId）  */
    @JvmField
    var activityId: String? = null

    /** 活动编码（接口字段 activityCode，replacePhoto 的 eventCode）  */
    @JvmField
    var activityCode: String? = null

    /** 证件缩略图 URL  */
    @JvmField
    var cardReduceImage: String? = null

    /** 业务编号 */
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

    /** 有效期开始时间（旧字段，兼容）  */
    @JvmField
    var validBegin: String? = null

    /** 有效期结束时间（旧字段，兼容）  */
    @JvmField
    var validEnd: String? = null

    /** 证件生效时间（getActiveUser 标准字段）  */
    @JvmField
    var cardEffectiveDate: String? = null

    /** 证件失效时间（getActiveUser 标准字段）  */
    @JvmField
    var cardExpirationDate: String? = null

    /** 证件发布状态：2-已发布，3-已取消发布  */
    @JvmField
    var cardPublishFlag: Int = 0

    /** 是否删除（0-正常，1-删除）  */
    @JvmField
    var deleted: String? = null

    /** 现场绑定实名证件：1-已绑定，0-未绑定  */
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

    /** 分项权限（与 matrixAuth.sportProject 对应，可选）  */
    @JvmField
    var sportPrivileges: String? = null

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
