package com.largeevent.management.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * /getMatrixAuthInfoList 设备通行权限项
 */
public class MatrixAuthInfoDTO {
    @SerializedName(value = "authId", alternate = {"AuthId", "authID"})
    public String authId;
    @SerializedName(value = "name", alternate = {"Name"})
    public String name;
    @SerializedName(value = "number", alternate = {"Number"})
    public String number;
    @SerializedName(value = "venue", alternate = {"Venue"})
    public String venue;
    @SerializedName(value = "venueVal", alternate = {"VenueVal"})
    public String venueVal;
    @SerializedName(value = "sportProject", alternate = {"SportProject"})
    public String sportProject;
    @SerializedName(value = "sportProjectVal", alternate = {"SportProjectVal"})
    public String sportProjectVal;
    @SerializedName(value = "venueArea", alternate = {"VenueArea"})
    public String venueArea;
    @SerializedName(value = "venueAreaVal", alternate = {"VenueAreaVal"})
    public String venueAreaVal;
    @SerializedName(value = "venuePartition", alternate = {"VenuePartition"})
    public String venuePartition;
    @SerializedName(value = "venuePartitionVal", alternate = {"VenuePartitionVal"})
    public String venuePartitionVal;
    @SerializedName(value = "seat", alternate = {"Seat"})
    public String seat;
    @SerializedName(value = "seatVal", alternate = {"SeatVal"})
    public String seatVal;
    @SerializedName(value = "other", alternate = {"Other"})
    public String other;
    @SerializedName(value = "otherVal", alternate = {"OtherVal"})
    public String otherVal;
    @SerializedName(value = "park", alternate = {"Park"})
    public String park;
    @SerializedName(value = "parkVal", alternate = {"ParkVal"})
    public String parkVal;
    @SerializedName(value = "securityColor", alternate = {"SecurityColor"})
    public String securityColor;
    @SerializedName(value = "securityColorVal", alternate = {"SecurityColorVal"})
    public String securityColorVal;
    @SerializedName(value = "type", alternate = {"Type"})
    public Integer type;
    @SerializedName(value = "eqpId", alternate = {"EqpId", "eqpID"})
    public String eqpId;
    @SerializedName(value = "serverUpdateTime", alternate = {"ServerUpdateTime"})
    public String serverUpdateTime;
    @SerializedName(value = "activityId", alternate = {"ActivityId"})
    public String activityId;
    @SerializedName(value = "activityCode", alternate = {"ActivityCode"})
    public String activityCode;
}
