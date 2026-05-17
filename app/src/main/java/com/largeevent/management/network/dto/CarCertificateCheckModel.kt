package com.largeevent.management.network.dto

import java.util.Date

/**
 * 车证核验记录模型
 */
class CarCertificateCheckModel {
    @JvmField var tagNo1: String? = null
    @JvmField var carPlate: String? = null
    @JvmField var deviceCode: String? = null
    @JvmField var location: String? = null
    @JvmField var passTime: Date? = null
    @JvmField var responsibilityPhone: String? = null
    @JvmField var organization: String? = null
    @JvmField var checkImg: String? = null
    @JvmField var cardType: String? = null
    @JvmField var licensePlateColor: String? = null
    @JvmField var startTime: String? = null
    @JvmField var endTime: String? = null
    @JvmField var accessauthority: String? = null
    @JvmField var event_status: String? = null
    @JvmField var checkStatus: Int? = null
    @JvmField var errorMsg: String? = null
    @JvmField var direction: String? = null
    @JvmField var createTime: Date? = null
}
