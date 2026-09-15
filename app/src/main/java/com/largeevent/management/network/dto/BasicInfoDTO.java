package com.largeevent.management.network.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 活动基础信息接收类（getBasicInfo 的 data）。
 * <p>
 * 接口：{@code GET androidNew/api/getBasicInfo}，位于 {@link ApiResponse#getData()}。
 * 含活动、场馆、位置、证件字典、matrixAuth、防疫等信息；字段名与接口 JSON 保持 camelCase。
 */
public class BasicInfoDTO {

    /** 活动列表 */
    public List<ActiveModelDTO> activeModelList;
    /** 活动场馆列表 */
    public List<ActiveVenueDTO> activeVenueModelList;
    /** 岗位/位置模型列表 */
    public List<PositionDTO> positionModelList;
    /** 通行规则列表 */
    public List<PassRuleDTO> passRuleModelList;
    /** 车证类型配置列表 */
    public List<CartTypeDTO> cartTypeModelList;
    /** 人证证件类型字典 */
    public List<CertTypeDTO> personCertTypeList;
    /** 车证证件类型字典 */
    public List<CertTypeDTO> carCertTypeList;
    /** 设备位置/分区列表（含 moduleType：1 人证 / 2 车证） */
    public List<LocationInfoDTO> locationInfoList;
    /** 基础信息中的设备权限矩阵（可能未按模块隔离，核验优先用 getMatrixAuthInfoList 缓存） */
    public List<MatrixAuthInfoDTO> matrixAuthInfoList;
    /** 防疫信息列表 */
    public List<EpidemicInfoDTO> epidemicInfoList;
    /** 场馆权限字典（设置页「场馆权限」芯片） */
    public List<DictItemDTO> venueInfoList;
    /** 分区权限字典（personCertZoneList，对应 matrixAuth.venuePartition / zonePrivileges） */
    public List<DictItemDTO> personCertZoneList;
    /** 区域权限字典（personCertAreaList，对应 matrixAuth.venueArea / areaPrivileges） */
    public List<DictItemDTO> personCertAreaList;

    /** 活动项 */
    public static class ActiveModelDTO {
        /** 活动 ID */
        public String id;
        /** 活动状态 */
        public String status;
        /** 活动名称 */
        public String activeName;
        /** 活动类型 */
        public String activeType;
        /** 活动场馆 ID */
        public String activeVenueId;
        /** 许可单位 */
        public String licenseUnit;
        /** 主办单位 */
        public String hostUnit;
        /** 活动规模 */
        public String activeScale;
        /** 安保负责人 */
        public String securityPerson;
        /** 查验负责人 */
        public String checkPerson;
        /** 签名/标识 */
        public String sign;
        /** 活动头像/图标 */
        public String avatar;
        /** 线路状态 */
        public Integer lineStatus;
        /** 映射活动 ID */
        public String ysActiveId;
        /** 制证/认证编码（部分接口作 eventCode） */
        public String accreditationCode;
        /** 监管 ID */
        public String supervisionId;
        /** 签到类型 */
        public String signType;
        /** 活动场次日期列表 */
        public List<ActiveDateDTO> activeDateModelList;
        /** 子活动/单元列表 */
        public List<ActiveUnitDTO> activeUnitModelList;
        /** 签到开始时间 */
        public String signStartTime;
        /** 签到结束时间 */
        public String signEndTime;
        /** 活动开始时间 */
        public String activeStartTime;
        /** 活动结束时间 */
        public String activeEndTime;
        /** 签到状态 */
        public int signStatus;
    }

    /** 活动场次日期 */
    public static class ActiveDateDTO {
        /** 场次日期，如 yyyy-MM-dd */
        public String date;
    }

    /** 子活动单元 */
    public static class ActiveUnitDTO {
        /** 子活动/单元名称 */
        public String unitName;
    }

    /** 活动场馆 */
    public static class ActiveVenueDTO {
        /** 场馆记录 ID */
        public String id;
        /** 场馆类型 */
        public String activeVenueType;
        /** 排序 */
        public String activeVenueSort;
        /** 场馆编码 */
        public String venueCode;
        /** GIS 坐标等信息 */
        public String gis;
        /** 场馆名称 */
        public String venueName;
        /** 场馆图片 */
        public String venuePicture;
        /** 地址 */
        public String address;
        /** 场馆下区域列表 */
        public List<ActiveAreaDTO> activeAreaList;
    }

    /** 场馆区域 */
    public static class ActiveAreaDTO {
        /** 区域 ID */
        public String id;
        /** 区域名称（优先） */
        public String areaName;
        /** 区域名称兼容字段 */
        public String name;
        /** 区域编码（优先） */
        public String areaCode;
        /** 区域编码兼容字段 */
        public String code;
    }

    /** 岗位/位置 */
    public static class PositionDTO {
        /** 岗位 ID */
        public String id;
        /** 岗位名称 */
        public String name;
        /** 岗位编码 */
        public String positionCode;
        /** 岗位类型 */
        public String positionType;
        /** 岗位描述 */
        public String positionDesc;
        /** 父岗位 ID */
        public String parentPositionId;
    }

    /** 通行规则 */
    public static class PassRuleDTO {
        /** 规则 ID */
        public String id;
        /** 规则编码 */
        public String code;
        /** 通行岗位编码 */
        public String passPositionCode;
        /** 时间类型 */
        public String timeType;
        /** 时间描述 */
        public String timeDesc;
    }

    /** 车证类型配置 */
    public static class CartTypeDTO {
        /** 配置 ID */
        public String id;
        /** 岗位编码 */
        public String positionCode;
        /** 子应用类型编码 */
        public String subAppTypeCode;
    }

    /** 证件类型字典项 */
    public static class CertTypeDTO {
        /** 排序号 */
        public String sortNumber;
        /** 字典类型 */
        public String dictType;
        /** 是否锁定 */
        public String isLocked;
        /** 字典显示值 */
        public String dictValue;
        /** 字典编码 */
        public String dictCode;
    }

    /** 设备位置/分区（locationType：cg=位置，fq=分区） */
    public static class LocationInfoDTO {
        /** 位置/分区 ID */
        public String locationId;
        /** 父级 ID */
        public String parentId;
        /** 编号 */
        public String locationNo;
        /** 名称 */
        public String locationName;
        /** 类型：cg 位置 / fq 分区 */
        public String locationType;
        /** 描述 */
        public String locationDesc;
        /** 活动 ID */
        public String activityId;
        /** 设备 ID */
        public String eqpId;
        /** 活动编码 */
        public String activityCode;
        /** 模块类型：1 人证 / 2 车证；0 表示通用 */
        public int moduleType;
    }

    /** 防疫信息 */
    public static class EpidemicInfoDTO {
        /** 防疫记录 ID */
        public String epidemicId;
        /** 登记号（可与证件号关联） */
        public String registrationNumber;
        /** 是否已检测 */
        public Integer isDetectedStatus;
        /** 密接状态 */
        public Integer closeContactStatus;
        /** 阳性状态 */
        public Integer positiveStatus;
        /** 推送时间 */
        public String pushTime;
        /** 接收时间 */
        public String receiveTime;
        /** 活动 ID */
        public String activityId;
        /** 活动编码 */
        public String activityCode;
        /** 预留字段 */
        public String rfu;
        /** 设备 ID */
        public String eqpId;
    }

    /**
     * 字典项（场馆权限 / 区域权限 / 分区权限等）。
     * 用于 venueInfoList、personCertZoneList、personCertAreaList。
     */
    public static class DictItemDTO {
        /** 字典项 ID */
        public String id;
        /** 创建人 */
        public String createBy;
        /** 创建时间 */
        public String createTime;
        /** 更新人 */
        public String updateBy;
        /** 更新时间 */
        public String updateTime;
        /** 是否删除 */
        public Integer isDeleted;
        /** 备注 */
        public String remark;
        /** 字典类型 */
        public String dictType;
        /** 字典编码（与 matrixAuth / 证件权限 code 比对） */
        public String dictCode;
        /** 字典显示名称 */
        public String dictValue;
        /** 排序号 */
        public Integer sortNumber;
        /** 是否锁定 */
        public Integer isLocked;
    }

    public static <T> List<T> safeList(List<T> list) {
        return list != null ? list : new ArrayList<T>();
    }
}
