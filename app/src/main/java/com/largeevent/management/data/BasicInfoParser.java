package com.largeevent.management.data;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.BasicInfo.ActiveAreaModel;
import com.largeevent.management.model.BasicInfo.ActiveDateModel;
import com.largeevent.management.model.BasicInfo.ActiveModel;
import com.largeevent.management.model.BasicInfo.ActiveUnitModel;
import com.largeevent.management.model.BasicInfo.ActiveVenueModel;
import com.largeevent.management.model.BasicInfo.CartTypeModel;
import com.largeevent.management.model.BasicInfo.CertTypeModel;
import com.largeevent.management.model.BasicInfo.EpidemicInfo;
import com.largeevent.management.model.BasicInfo.LocationInfo;
import com.largeevent.management.model.BasicInfo.MatrixAuthInfo;
import com.largeevent.management.model.BasicInfo.PassRuleModel;
import com.largeevent.management.model.BasicInfo.PositionModel;
import com.largeevent.management.model.BasicInfo.VenueInfo;
import com.largeevent.management.network.dto.BasicInfoVo;
import com.largeevent.management.network.dto.BasicInfoVo.ActiveAreaVo;
import com.largeevent.management.network.dto.BasicInfoVo.ActiveDateVo;
import com.largeevent.management.network.dto.BasicInfoVo.ActiveModelVo;
import com.largeevent.management.network.dto.BasicInfoVo.ActiveUnitVo;
import com.largeevent.management.network.dto.BasicInfoVo.ActiveVenueVo;
import com.largeevent.management.network.dto.BasicInfoVo.CartTypeVo;
import com.largeevent.management.network.dto.BasicInfoVo.CertTypeVo;
import com.largeevent.management.network.dto.BasicInfoVo.DictItemVo;
import com.largeevent.management.network.dto.BasicInfoVo.EpidemicInfoVo;
import com.largeevent.management.network.dto.BasicInfoVo.LocationInfoVo;
import com.largeevent.management.network.dto.BasicInfoVo.PassRuleVo;
import com.largeevent.management.network.dto.BasicInfoVo.PositionVo;
import com.largeevent.management.network.dto.MatrixAuthInfoVo;

import java.util.ArrayList;
import java.util.List;

/**
 * getBasicInfo：Gson 反序列化 DTO → 领域模型 {@link BasicInfo}。
 */
public final class BasicInfoParser {

    private static final Gson GSON = new Gson();

    private BasicInfoParser() {
    }

    static BasicInfo parse(String payload) {
        BasicInfoVo dto = parseDto(payload);
        return fromDto(dto);
    }

    /** 解析接口 data JSON（兼容外层包了 code/data 的旧缓存） */
    public static BasicInfoVo parseDto(String payload) {
        if (TextUtils.isEmpty(payload)) {
            return new BasicInfoVo();
        }
        try {
            JsonElement element = JsonParser.parseString(payload);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                if (root.has("data") && root.get("data").isJsonObject()) {
                    element = root.get("data");
                }
            }
            BasicInfoVo dto = GSON.fromJson(element, BasicInfoVo.class);
            return dto != null ? dto : new BasicInfoVo();
        } catch (Exception e) {
            return new BasicInfoVo();
        }
    }

    public static BasicInfo fromDto(BasicInfoVo dto) {
        if (dto == null) {
            return emptyBasicInfo();
        }
        return new BasicInfo(
                mapActiveModels(dto.activeModelList),
                mapVenues(dto.activeVenueModelList),
                mapPositions(dto.positionModelList),
                mapPassRules(dto.passRuleModelList),
                mapCartTypes(dto.cartTypeModelList),
                mapCertTypes(dto.personCertTypeList),
                mapCertTypes(dto.carCertTypeList),
                mapLocations(dto.locationInfoList),
                mapMatrixAuth(dto.matrixAuthInfoList),
                mapEpidemics(dto.epidemicInfoList),
                mapDictItems(dto.venueInfoList),
                mapDictItems(dto.personCertZoneList),
                mapDictItems(dto.personCertAreaList)
        );
    }

    private static BasicInfo emptyBasicInfo() {
        return new BasicInfo(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private static List<ActiveModel> mapActiveModels(List<ActiveModelVo> list) {
        List<ActiveModel> result = new ArrayList<>();
        for (ActiveModelVo obj : BasicInfoVo.safeList(list)) {
            if (obj == null) {
                continue;
            }
            List<ActiveDateModel> dates = new ArrayList<>();
            for (ActiveDateVo d : BasicInfoVo.safeList(obj.activeDateModelList)) {
                if (d != null) {
                    dates.add(new ActiveDateModel(d.date));
                }
            }
            List<ActiveUnitModel> units = new ArrayList<>();
            for (ActiveUnitVo u : BasicInfoVo.safeList(obj.activeUnitModelList)) {
                if (u != null) {
                    units.add(new ActiveUnitModel(u.unitName));
                }
            }
            result.add(new ActiveModel(
                    obj.id, obj.status, obj.activeName, obj.activeType, obj.activeVenueId,
                    obj.licenseUnit, obj.hostUnit, obj.activeScale, obj.securityPerson,
                    obj.checkPerson, obj.sign, obj.avatar, obj.lineStatus, obj.ysActiveId,
                    obj.accreditationCode, obj.supervisionId, obj.signType,
                    dates, units,
                    obj.signStartTime, obj.signEndTime, obj.activeStartTime, obj.activeEndTime,
                    obj.signStatus
            ));
        }
        return result;
    }

    private static List<ActiveVenueModel> mapVenues(List<ActiveVenueVo> list) {
        List<ActiveVenueModel> result = new ArrayList<>();
        for (ActiveVenueVo obj : BasicInfoVo.safeList(list)) {
            if (obj == null) {
                continue;
            }
            List<ActiveAreaModel> areas = new ArrayList<>();
            for (ActiveAreaVo a : BasicInfoVo.safeList(obj.activeAreaList)) {
                if (a == null) {
                    continue;
                }
                String areaName = !TextUtils.isEmpty(a.areaName) ? a.areaName : a.name;
                String areaCode = !TextUtils.isEmpty(a.areaCode) ? a.areaCode : a.code;
                areas.add(new ActiveAreaModel(a.id, areaName, areaCode));
            }
            result.add(new ActiveVenueModel(
                    obj.id, obj.activeVenueType, obj.activeVenueSort, obj.venueCode, obj.gis,
                    obj.venueName, obj.venuePicture, obj.address, areas
            ));
        }
        return result;
    }

    private static List<PositionModel> mapPositions(List<PositionVo> list) {
        List<PositionModel> result = new ArrayList<>();
        for (PositionVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new PositionModel(
                        obj.id, obj.name, obj.positionCode, obj.positionType,
                        obj.positionDesc, obj.parentPositionId));
            }
        }
        return result;
    }

    private static List<PassRuleModel> mapPassRules(List<PassRuleVo> list) {
        List<PassRuleModel> result = new ArrayList<>();
        for (PassRuleVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new PassRuleModel(
                        obj.id, obj.code, obj.passPositionCode, obj.timeType, obj.timeDesc));
            }
        }
        return result;
    }

    private static List<CartTypeModel> mapCartTypes(List<CartTypeVo> list) {
        List<CartTypeModel> result = new ArrayList<>();
        for (CartTypeVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new CartTypeModel(obj.id, obj.positionCode, obj.subAppTypeCode));
            }
        }
        return result;
    }

    private static List<CertTypeModel> mapCertTypes(List<CertTypeVo> list) {
        List<CertTypeModel> result = new ArrayList<>();
        for (CertTypeVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new CertTypeModel(
                        obj.sortNumber, obj.dictType, obj.isLocked, obj.dictValue, obj.dictCode));
            }
        }
        return result;
    }

    private static List<LocationInfo> mapLocations(List<LocationInfoVo> list) {
        List<LocationInfo> result = new ArrayList<>();
        for (LocationInfoVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new LocationInfo(
                        obj.locationId, obj.parentId, obj.locationNo, obj.locationName,
                        obj.locationType, obj.locationDesc, obj.activityId, obj.eqpId,
                        obj.activityCode, obj.moduleType));
            }
        }
        return result;
    }

    private static List<MatrixAuthInfo> mapMatrixAuth(List<MatrixAuthInfoVo> list) {
        List<MatrixAuthInfo> result = new ArrayList<>();
        for (MatrixAuthInfoVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new MatrixAuthInfo(
                        obj.authId, obj.name, obj.number, obj.venue, obj.venueVal,
                        obj.sportProject, obj.sportProjectVal, obj.venueArea, obj.venueAreaVal,
                        obj.venuePartition, obj.venuePartitionVal, obj.seat, obj.seatVal,
                        obj.other, obj.otherVal, obj.park, obj.parkVal,
                        obj.securityColor, obj.securityColorVal, obj.type, obj.eqpId,
                        obj.serverUpdateTime, obj.activityId, obj.activityCode));
            }
        }
        return result;
    }

    private static List<EpidemicInfo> mapEpidemics(List<EpidemicInfoVo> list) {
        List<EpidemicInfo> result = new ArrayList<>();
        for (EpidemicInfoVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new EpidemicInfo(
                        obj.epidemicId, obj.registrationNumber, obj.isDetectedStatus,
                        obj.closeContactStatus, obj.positiveStatus, obj.pushTime,
                        obj.receiveTime, obj.activityId, obj.activityCode, obj.rfu, obj.eqpId));
            }
        }
        return result;
    }

    private static List<VenueInfo> mapDictItems(List<DictItemVo> list) {
        List<VenueInfo> result = new ArrayList<>();
        for (DictItemVo obj : BasicInfoVo.safeList(list)) {
            if (obj != null) {
                result.add(new VenueInfo(
                        obj.id, obj.createBy, obj.createTime, obj.updateBy, obj.updateTime,
                        obj.isDeleted, obj.remark, obj.dictType, obj.dictCode, obj.dictValue,
                        obj.sortNumber, obj.isLocked));
            }
        }
        return result;
    }
}
