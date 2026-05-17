package com.largeevent.management.model

import java.io.*
import java.util.*

class BasicInfo(
    activeModels: MutableList<ActiveModel?>?,
    activeVenues: MutableList<ActiveVenueModel?>?,
    positions: MutableList<PositionModel?>?,
    passRules: MutableList<PassRuleModel?>?,
    cartTypes: MutableList<CartTypeModel?>?,
    personCertTypes: MutableList<CertTypeModel?>?,
    carCertTypes: MutableList<CertTypeModel?>?,
    locationInfoList: MutableList<LocationInfo?>?,
    matrixAuthInfoList: MutableList<MatrixAuthInfo?>?,
    epidemicInfoList: MutableList<EpidemicInfo?>?,
    venueInfoList: MutableList<VenueInfo?>?
) : Serializable {
    private val activeModels: MutableList<ActiveModel?>
    private val activeVenues: MutableList<ActiveVenueModel?>
    private val positions: MutableList<PositionModel?>
    private val passRules: MutableList<PassRuleModel?>
    private val cartTypes: MutableList<CartTypeModel?>
    private val personCertTypes: MutableList<CertTypeModel?>
    private val carCertTypes: MutableList<CertTypeModel?>
    private val locationInfoList: MutableList<LocationInfo?>
    private val matrixAuthInfoList: MutableList<MatrixAuthInfo?>
    private val epidemicInfoList: MutableList<EpidemicInfo?>
    private val venueInfoList: MutableList<VenueInfo?>

    init {
        this.activeModels =
            if (activeModels == null) ArrayList<ActiveModel?>() else ArrayList<ActiveModel?>(
                activeModels
            )
        this.activeVenues =
            if (activeVenues == null) ArrayList<ActiveVenueModel?>() else ArrayList<ActiveVenueModel?>(
                activeVenues
            )
        this.positions =
            if (positions == null) ArrayList<PositionModel?>() else ArrayList<PositionModel?>(
                positions
            )
        this.passRules =
            if (passRules == null) ArrayList<PassRuleModel?>() else ArrayList<PassRuleModel?>(
                passRules
            )
        this.cartTypes =
            if (cartTypes == null) ArrayList<CartTypeModel?>() else ArrayList<CartTypeModel?>(
                cartTypes
            )
        this.personCertTypes =
            if (personCertTypes == null) ArrayList<CertTypeModel?>() else ArrayList<CertTypeModel?>(
                personCertTypes
            )
        this.carCertTypes =
            if (carCertTypes == null) ArrayList<CertTypeModel?>() else ArrayList<CertTypeModel?>(
                carCertTypes
            )
        this.locationInfoList =
            if (locationInfoList == null) ArrayList<LocationInfo?>() else ArrayList<LocationInfo?>(
                locationInfoList
            )
        this.matrixAuthInfoList =
            if (matrixAuthInfoList == null) ArrayList<MatrixAuthInfo?>() else ArrayList<MatrixAuthInfo?>(
                matrixAuthInfoList
            )
        this.epidemicInfoList =
            if (epidemicInfoList == null) ArrayList<EpidemicInfo?>() else ArrayList<EpidemicInfo?>(
                epidemicInfoList
            )
        this.venueInfoList =
            if (venueInfoList == null) ArrayList<VenueInfo?>() else ArrayList<VenueInfo?>(
                venueInfoList
            )
    }

    fun getActiveModels(): MutableList<ActiveModel?> {
        return Collections.unmodifiableList<ActiveModel?>(activeModels)
    }

    fun getActiveVenues(): MutableList<ActiveVenueModel?> {
        return Collections.unmodifiableList<ActiveVenueModel?>(activeVenues)
    }

    fun getPositions(): MutableList<PositionModel?> {
        return Collections.unmodifiableList<PositionModel?>(positions)
    }

    fun getPassRules(): MutableList<PassRuleModel?> {
        return Collections.unmodifiableList<PassRuleModel?>(passRules)
    }

    fun getCartTypes(): MutableList<CartTypeModel?> {
        return Collections.unmodifiableList<CartTypeModel?>(cartTypes)
    }

    fun getPersonCertTypes(): MutableList<CertTypeModel?> {
        return Collections.unmodifiableList<CertTypeModel?>(personCertTypes)
    }

    fun getCarCertTypes(): MutableList<CertTypeModel?> {
        return Collections.unmodifiableList<CertTypeModel?>(carCertTypes)
    }

    fun getLocationInfoList(): MutableList<LocationInfo?> {
        return Collections.unmodifiableList<LocationInfo?>(locationInfoList)
    }

    fun getMatrixAuthInfoList(): MutableList<MatrixAuthInfo?> {
        return Collections.unmodifiableList<MatrixAuthInfo?>(matrixAuthInfoList)
    }

    fun getEpidemicInfoList(): MutableList<EpidemicInfo?> {
        return Collections.unmodifiableList<EpidemicInfo?>(epidemicInfoList)
    }

    fun getVenueInfoList(): MutableList<VenueInfo?> {
        return Collections.unmodifiableList<VenueInfo?>(venueInfoList)
    }

    val isEmpty: Boolean
        get() = activeModels.isEmpty() && activeVenues.isEmpty() && positions.isEmpty() && 
                passRules.isEmpty() && cartTypes.isEmpty() && personCertTypes.isEmpty() && 
                carCertTypes.isEmpty() && locationInfoList.isEmpty() && matrixAuthInfoList.isEmpty() &&
                epidemicInfoList.isEmpty() && venueInfoList.isEmpty()

    class ActiveModel(
        @JvmField val id: String?,
        val status: String?,
        @JvmField val activeName: String?,
        val activeType: String?,
        val activeVenueId: String?,
        val licenseUnit: String?,
        val hostUnit: String?,
        val activeScale: String?,
        val securityPerson: String?,
        val checkPerson: String?,
        val sign: String?,
        val avatar: String?,
        val lineStatus: Int?,
        val ysActiveId: String?,
        @JvmField val accreditationCode: String?,
        val supervisionId: String?,
        val signType: String?,
        activeDateModelList: MutableList<ActiveDateModel?>?,
        activeUnitModelList: MutableList<ActiveUnitModel?>?,
        val signStartTime: String?,
        val signEndTime: String?,
        val activeStartTime: String?,
        val activeEndTime: String?,
        val signStatus: Int
    ) : Serializable {
        private val activeDateModelList: MutableList<ActiveDateModel?>
        private val activeUnitModelList: MutableList<ActiveUnitModel?>

        init {
            this.activeDateModelList =
                if (activeDateModelList == null) ArrayList<ActiveDateModel?>()
                else ArrayList<ActiveDateModel?>(activeDateModelList)
            this.activeUnitModelList =
                if (activeUnitModelList == null) ArrayList<ActiveUnitModel?>()
                else ArrayList<ActiveUnitModel?>(activeUnitModelList)
        }

        fun getActiveDateModelList(): MutableList<ActiveDateModel?> {
            return Collections.unmodifiableList<ActiveDateModel?>(activeDateModelList)
        }

        fun getActiveUnitModelList(): MutableList<ActiveUnitModel?> {
            return Collections.unmodifiableList<ActiveUnitModel?>(activeUnitModelList)
        }
    }

    class ActiveDateModel(@JvmField val date: String?) : Serializable

    class ActiveUnitModel(val unitName: String?) : Serializable

    class ActiveVenueModel(
        @JvmField val id: String?,
        val activeVenueType: String?,
        val activeVenueSort: String?,
        @JvmField val venueCode: String?,
        val gis: String?,
        @JvmField val venueName: String?,
        val venuePicture: String?,
        val address: String?,
        activeAreaList: MutableList<ActiveAreaModel?>?
    ) : Serializable {
        private val activeAreaList: MutableList<ActiveAreaModel?>

        init {
            this.activeAreaList = if (activeAreaList == null) ArrayList<ActiveAreaModel?>()
            else ArrayList<ActiveAreaModel?>(activeAreaList)
        }

        fun getActiveAreaList(): MutableList<ActiveAreaModel?> {
            return Collections.unmodifiableList<ActiveAreaModel?>(activeAreaList)
        }
    }

    class ActiveAreaModel(val id: String?, @JvmField val areaName: String?, @JvmField val areaCode: String?) :
        Serializable

    class PositionModel(
        val id: String?,
        @JvmField val name: String?,
        @JvmField val positionCode: String?,
        @JvmField val positionType: String?,
        @JvmField val positionDesc: String?,
        @JvmField val parentPositionId: String?
    ) : Serializable

    class PassRuleModel(
        @JvmField val id: String?,
        @JvmField val code: String?,
        @JvmField val passPositionCode: String?,
        @JvmField val timeType: String?,
        @JvmField val timeDesc: String?
    ) : Serializable

    class CartTypeModel(val id: String?, val positionCode: String?, val subAppTypeCode: String?) :
        Serializable

    class CertTypeModel(
        @JvmField val sortNumber: String?,
        @JvmField val dictType: String?,
        @JvmField val isLocked: String?,
        @JvmField val dictValue: String?,
        @JvmField val dictCode: String?
    ) : Serializable

    class LocationInfo(
        @JvmField val locationId: String?,
        @JvmField val parentId: String?,
        @JvmField val locationNo: String?,
        @JvmField val locationName: String?,
        @JvmField val locationType: String?,
        @JvmField val locationDesc: String?,
        @JvmField val activityId: String?,
        @JvmField val eqpId: String?,
        @JvmField val activityCode: String?
    ) : Serializable

    class MatrixAuthInfo(
        @JvmField val authId: String?,
        @JvmField val name: String?,
        @JvmField val number: String?,
        @JvmField val venue: String?,
        @JvmField val venueVal: String?,
        @JvmField val sportProject: String?,
        @JvmField val sportProjectVal: String?,
        @JvmField val venueArea: String?,
        @JvmField val venueAreaVal: String?,
        @JvmField val venuePartition: String?,
        @JvmField val venuePartitionVal: String?,
        @JvmField val seat: String?,
        @JvmField val seatVal: String?,
        @JvmField val other: String?,
        @JvmField val otherVal: String?,
        @JvmField val park: String?,
        @JvmField val parkVal: String?,
        @JvmField val securityColor: String?,
        @JvmField val securityColorVal: String?,
        @JvmField val type: Int?,
        @JvmField val eqpId: String?,
        @JvmField val serverUpdateTime: String?,
        @JvmField val activityId: String?,
        @JvmField val activityCode: String?
    ) : Serializable

    class EpidemicInfo(
        @JvmField val epidemicId: String?,
        @JvmField val registrationNumber: String?,
        @JvmField val isDetectedStatus: Int?,
        @JvmField val closeContactStatus: Int?,
        @JvmField val positiveStatus: Int?,
        @JvmField val pushTime: String?,
        @JvmField val receiveTime: String?,
        @JvmField val activityId: String?,
        @JvmField val activityCode: String?,
        @JvmField val rfu: String?,
        @JvmField val eqpId: String?
    ) : Serializable

    class VenueInfo(
        @JvmField val id: String?,
        @JvmField val createBy: String?,
        @JvmField val createTime: String?,
        @JvmField val updateBy: String?,
        @JvmField val updateTime: String?,
        @JvmField val isDeleted: Int?,
        @JvmField val remark: String?,
        @JvmField val dictType: String?,
        @JvmField val dictCode: String?,
        @JvmField val dictValue: String?,
        @JvmField val sortNumber: Int?,
        @JvmField val isLocked: Int?
    ) : Serializable
}
