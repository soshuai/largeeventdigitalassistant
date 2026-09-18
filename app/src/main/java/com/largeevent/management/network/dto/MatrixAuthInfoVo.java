package com.largeevent.management.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 设备通行权限项接收类（matrixAuth）。
 * <p>
 * 接口：
 * <ul>
 *   <li>{@code GET androidNew/api/getMatrixAuthInfoList} — 人证 {@link ApiResponse#getData()} 列表元素</li>
 *   <li>{@code GET androidNew/api/getCarMatrixAuthList} — 车证，入参返参与人证接口一致</li>
 *   <li>{@code GET androidNew/api/getBasicInfo} — {@link BasicInfoVo#matrixAuthInfoList} 元素</li>
 * </ul>
 * 含场馆 / 分项 / 分区 / 区域等权限 code 与展示名。
 */
public class MatrixAuthInfoVo {
    /** 权限记录 ID */
    @SerializedName(value = "authId", alternate = {"AuthId", "authID"})
    public String authId;
    /** 权限名称 */
    @SerializedName(value = "name", alternate = {"Name"})
    public String name;
    /** 权限编号 */
    @SerializedName(value = "number", alternate = {"Number"})
    public String number;
    /** 场馆权限 code（如 BQG；ALL/INF 表示全部） */
    @SerializedName(value = "venue", alternate = {"Venue"})
    public String venue;
    /** 场馆权限显示名 */
    @SerializedName(value = "venueVal", alternate = {"VenueVal"})
    public String venueVal;
    /** 分项/运动项目权限 code */
    @SerializedName(value = "sportProject", alternate = {"SportProject"})
    public String sportProject;
    /** 分项权限显示名 */
    @SerializedName(value = "sportProjectVal", alternate = {"SportProjectVal"})
    public String sportProjectVal;
    /** 区域权限 code（对应 personCertAreaList / areaPrivileges） */
    @SerializedName(value = "venueArea", alternate = {"VenueArea"})
    public String venueArea;
    /** 区域权限显示名 */
    @SerializedName(value = "venueAreaVal", alternate = {"VenueAreaVal"})
    public String venueAreaVal;
    /** 分区权限 code（对应 personCertZoneList / zonePrivileges） */
    @SerializedName(value = "venuePartition", alternate = {"VenuePartition"})
    public String venuePartition;
    /** 分区权限显示名 */
    @SerializedName(value = "venuePartitionVal", alternate = {"VenuePartitionVal"})
    public String venuePartitionVal;
    /** 座位权限 code */
    @SerializedName(value = "seat", alternate = {"Seat"})
    public String seat;
    /** 座位权限显示名 */
    @SerializedName(value = "seatVal", alternate = {"SeatVal"})
    public String seatVal;
    /** 其他权限 code */
    @SerializedName(value = "other", alternate = {"Other"})
    public String other;
    /** 其他权限显示名 */
    @SerializedName(value = "otherVal", alternate = {"OtherVal"})
    public String otherVal;
    /** 停车/车证相关权限 code */
    @SerializedName(value = "park", alternate = {"Park"})
    public String park;
    /** 停车权限显示名 */
    @SerializedName(value = "parkVal", alternate = {"ParkVal"})
    public String parkVal;
    /** 安检颜色 code */
    @SerializedName(value = "securityColor", alternate = {"SecurityColor"})
    public String securityColor;
    /** 安检颜色显示名 */
    @SerializedName(value = "securityColorVal", alternate = {"SecurityColorVal"})
    public String securityColorVal;
    /** 权限类型 */
    @SerializedName(value = "type", alternate = {"Type"})
    public Integer type;
    /** 设备 ID */
    @SerializedName(value = "eqpId", alternate = {"EqpId", "eqpID"})
    public String eqpId;
    /** 服务端更新时间 */
    @SerializedName(value = "serverUpdateTime", alternate = {"ServerUpdateTime"})
    public String serverUpdateTime;
    /** 活动 ID */
    @SerializedName(value = "activityId", alternate = {"ActivityId"})
    public String activityId;
    /** 活动编码 */
    @SerializedName(value = "activityCode", alternate = {"ActivityCode"})
    public String activityCode;
}
