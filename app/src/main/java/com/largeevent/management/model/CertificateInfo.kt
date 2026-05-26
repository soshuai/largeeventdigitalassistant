package com.largeevent.management.model

import android.text.*
import java.io.*
import java.text.*
import java.util.*

class CertificateInfo(
    @JvmField val certId: String?,
    @JvmField val name: String?,
    @JvmField val number: String?,
    @JvmField val documentType: String?,
    /** 身份证类型（对应 identityDocumentType） */
    @JvmField val identityDocumentType: String?,
    /** 身份证号码（对应 identityDocumentNumber） */
    @JvmField val identityDocumentNumber: String?,
    @JvmField val chipId: String?,
    @JvmField val cardSerial: String?,
    @JvmField val validFrom: String?,
    @JvmField val validTo: String?,
    val isNeedBinding: Boolean,
    val isBound: Boolean,
    val isRealNameRequired: Boolean,
    areaPermissions: MutableSet<String?>?,
    @JvmField val photoUrl: String?,
    @JvmField val passRuleCode: String?,
    @JvmField val venueCode: String?,
    @JvmField val venuePrivileges: String?,
    @JvmField val areaPrivileges: String?,
    @JvmField val zonePrivileges: String?,
    @JvmField val sportPrivileges: String?,
    @JvmField val effectiveDateOfDayPass: String?
) : Serializable {
    @JvmField
    val areaPermissions: MutableSet<String?>

    init {
        this.areaPermissions =
            if (areaPermissions == null) mutableSetOf<String?>() else areaPermissions
    }

    fun hasPermission(requiredZone: String?): Boolean {
        if (TextUtils.isEmpty(requiredZone)) {
            return true
        }
        if (areaPermissions.isEmpty()) {
            return false
        }
        if (areaPermissions.contains("ALL")) {
            return true
        }
        return areaPermissions.contains(requiredZone)
    }

    fun isWithinValidTime(nowMillis: Long): Boolean {
        val from = parseTime(validFrom, Long.Companion.MIN_VALUE)
        val to = parseTime(validTo, Long.Companion.MAX_VALUE)
        return nowMillis >= from && nowMillis <= to
    }

    fun isTodayValid(nowMillis: Long): Boolean {
        if (TextUtils.isEmpty(effectiveDateOfDayPass)) {
            return true
        }
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date(nowMillis))
            var dayPass = effectiveDateOfDayPass!!.trim()
            if (dayPass.length >= 10) {
                dayPass = dayPass.substring(0, 10)
            }
            return todayStr == dayPass
        } catch (e: Exception) {
            return true
        }
    }

    private fun parseTime(time: String?, defaultValue: Long): Long {
        if (TextUtils.isEmpty(time)) {
            return defaultValue
        }
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(time ?: "")
            if (date != null) {
                return date.getTime()
            }
        } catch (e: ParseException) { // ignore
        }
        return defaultValue
    }

    class Builder {
        private var certId: String? = null
        private var name: String? = null
        private var number: String? = null
        private var documentType: String? = null
        private var identityDocumentType: String? = null
        private var identityDocumentNumber: String? = null
        private var chipId: String? = null
        private var cardSerial: String? = null
        private var validFrom: String? = null
        private var validTo: String? = null
        private var needBinding = false
        private var bound = false
        private var realNameRequired = true
        private val areaPermissions: MutableSet<String?> = HashSet<String?>()
        private var photoUrl: String? = null
        private var passRuleCode: String? = null
        private var venueCode: String? = null
        private var venuePrivileges: String? = null
        private var areaPrivileges: String? = null
        private var zonePrivileges: String? = null
        private var sportPrivileges: String? = null
        private var effectiveDateOfDayPass: String? = null

        fun setCertId(certId: String?): Builder {
            this.certId = certId
            return this
        }

        fun setName(name: String?): Builder {
            this.name = name
            return this
        }

        fun setNumber(number: String?): Builder {
            this.number = number
            return this
        }

        fun setDocumentType(documentType: String?): Builder {
            this.documentType = documentType
            return this
        }

        fun setIdentityDocumentType(identityDocumentType: String?): Builder {
            this.identityDocumentType = identityDocumentType
            return this
        }

        fun setIdentityDocumentNumber(identityDocumentNumber: String?): Builder {
            this.identityDocumentNumber = identityDocumentNumber
            return this
        }

        fun setChipId(chipId: String?): Builder {
            this.chipId = chipId
            return this
        }

        fun setCardSerial(cardSerial: String?): Builder {
            this.cardSerial = cardSerial
            return this
        }

        fun setValidFrom(validFrom: String?): Builder {
            this.validFrom = validFrom
            return this
        }

        fun setValidTo(validTo: String?): Builder {
            this.validTo = validTo
            return this
        }

        fun setNeedBinding(needBinding: Boolean): Builder {
            this.needBinding = needBinding
            return this
        }

        fun setBound(bound: Boolean): Builder {
            this.bound = bound
            return this
        }

        fun setRealNameRequired(realNameRequired: Boolean): Builder {
            this.realNameRequired = realNameRequired
            return this
        }

        fun addPermission(permission: String?): Builder {
            if (!TextUtils.isEmpty(permission)) {
                areaPermissions.add(permission)
            }
            return this
        }

        fun setPhotoUrl(photoUrl: String?): Builder {
            this.photoUrl = photoUrl
            return this
        }

        fun setPassRuleCode(passRuleCode: String?): Builder {
            this.passRuleCode = passRuleCode
            return this
        }

        fun setVenueCode(venueCode: String?): Builder {
            this.venueCode = venueCode
            return this
        }

        fun setVenuePrivileges(venuePrivileges: String?): Builder {
            this.venuePrivileges = venuePrivileges
            return this
        }

        fun setAreaPrivileges(areaPrivileges: String?): Builder {
            this.areaPrivileges = areaPrivileges
            return this
        }

        fun setZonePrivileges(zonePrivileges: String?): Builder {
            this.zonePrivileges = zonePrivileges
            return this
        }

        fun setSportPrivileges(sportPrivileges: String?): Builder {
            this.sportPrivileges = sportPrivileges
            return this
        }

        fun setEffectiveDateOfDayPass(effectiveDateOfDayPass: String?): Builder {
            this.effectiveDateOfDayPass = effectiveDateOfDayPass
            return this
        }

        fun build(): CertificateInfo {
            return CertificateInfo(
                certId,
                name,
                number,
                documentType,
                identityDocumentType,
                identityDocumentNumber,
                chipId,
                cardSerial,
                validFrom,
                validTo,
                needBinding,
                bound,
                realNameRequired,
                areaPermissions,
                photoUrl,
                passRuleCode,
                venueCode,
                venuePrivileges,
                areaPrivileges,
                zonePrivileges,
                sportPrivileges,
                effectiveDateOfDayPass
            )
        }
    }
}
