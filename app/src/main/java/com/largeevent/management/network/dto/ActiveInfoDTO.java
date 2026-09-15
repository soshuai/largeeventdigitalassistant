package com.largeevent.management.network.dto;

/**
 * 活动列表项接收类。
 * <p>
 * 接口：{@code GET androidNew/api/getActiveInfo}，位于 {@link ApiResponse#getData()} 的列表元素。
 * 字段与接口 JSON 保持 camelCase 一致。
 */
public class ActiveInfoDTO {
    /** 活动 ID */
    public String id;
    /** 创建人 */
    public String createBy;
    /** 创建时间 */
    public String createTime;
    /** 更新人 */
    public String updateBy;
    /** 更新时间 */
    public String updateTime;
    /** 是否删除：0 否 / 1 是 */
    public int isDeleted;
    /** 备注 */
    public String remark;
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
    /** 制证/认证编码 */
    public String accreditationCode;
    /** 监管 ID */
    public String supervisionId;
    /** 签到类型 */
    public String signType;
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
