package com.largeevent.management.network.dto

import java.util.Date

/**
 * 人证核验记录模型
 */
class PersonCardCheckModel {
    @JvmField var idCard: String? = null
    @JvmField var activeCode: String? = null
    @JvmField var activeId: String? = null
    @JvmField var userId: String? = null
    @JvmField var personType: String? = null
    @JvmField var personName: String? = null
    @JvmField var sex: String? = null
    @JvmField var unitName: String? = null
    @JvmField var phone: String? = null
    @JvmField var personPhoto: String? = null
    @JvmField var subAppTypeCode: String? = null
    @JvmField var tagNo: String? = null
    @JvmField var session: String? = null
    @JvmField var deviceId: String? = null
    @JvmField var deviceName: String? = null
    @JvmField var location: String? = null
    @JvmField var passRuleCode: String? = null
    @JvmField var passPositionCode: String? = null
    @JvmField var certNumber: String? = null
    @JvmField var passTime: Date? = null
    @JvmField var direction: String? = null
    @JvmField var checkImg: String? = null
    @JvmField var checkStatus: Int? = null
    @JvmField var errorMsg: String? = null
    @JvmField var createTime: Date? = null
}
