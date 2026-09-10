package com.largeevent.management.network

import com.largeevent.management.model.BasicInfo
import com.largeevent.management.network.dto.*
import okhttp3.*
import retrofit2.Call
import retrofit2.http.*

/**
 * 统一 API 服务接口
 * 集中管理所有后端接口调用
 */
interface ApiService {
    // ==================== 基础信息接口 ====================
    /**
     * 获取活动信息列表
     * @return 活动信息列表
     */
    @GET("androidNew/api/getActiveInfo")
    fun getActiveInfo(): Call<ApiResponse<MutableList<ActiveInfoDTO?>?>?>?

    /**
     * 获取基础信息
     * @param activityId 活动ID（必填）
     * @param eqpId 设备ID（必填）
     * @return 基础信息
     */
    @GET("androidNew/api/getBasicInfo")
    fun getBasicInfo(
        @Query("activityId") activityId: String?,
        @Query("eqpId") eqpId: String?
    ): Call<ResponseBody?>?

    /**
     * 获取设备通行权限信息
     * @param activityId 活动ID
     * @param eqpId 设备ID
     */
    @GET("androidNew/api/getMatrixAuthInfoList")
    fun getMatrixAuthInfoList(
        @Query("activityId") activityId: String?,
        @Query("eqpId") eqpId: String?
    ): Call<ApiResponse<MutableList<MatrixAuthInfoDTO?>?>?>?

    /**
     * 获取活动人员数据
     * @param activeId 活动ID（必填）
     * @param chipid 芯片号（可选，用于查询单个用户）
     * @return 人员数据列表
     */
    @GET("androidNew/api/getActiveUser")
    fun getActiveUser(@Query("activeId") activeId: String?, @Query("chipid") chipid: String?):
            Call<ApiResponse<MutableList<ActiveUserBaseDTO?>?>?>?

    /**
     * 根据证件号码查询证件信息（仅回显，不核验上传）
     * @param idNumber 证件号码
     * @param idType 证件类型（如身份证 169）
     */
    @GET("androidNew/api/getActiveUserByIdNumber")
    fun getActiveUserByIdNumber(
        @Query("idNumber") idNumber: String?,
        @Query("idType") idType: String?
    ): Call<ApiResponse<MutableList<ActiveUserBaseDTO?>?>?>?

    /**
     * 获取活动车证详情
     * @param activeId 活动ID（必填）
     * @param chipid 车证芯片号（IC物理卡号）
     * @return 车证数据列表
     */
    @GET("androidNew/api/getActiveCarCert")
    fun getActiveCarCert(@Query("activeId") activeId: String?, @Query("chipid") chipid: String?):
            Call<ApiResponse<MutableList<CarCertificateDTO?>?>?>?
    /**
     * 上传人证核验结果（JSON 体字段与 receiveCheckPerson 文档一致）
     */
    @POST("androidNew/api/receiveCheckPerson")
    fun receiveCheckPerson(@Body body: ReceiveCheckPersonDTO?): Call<ResponseBody?>?

    /**
     * 上传车证核验结果
     * @param requestMap 包含data字段的Map
     * @return 响应体
     */
    @POST("androidNew/api/receiveCheckCar")
    fun receiveCheckCar(@Body requestMap: Map<String, String>?): Call<ResponseBody?>?

    /**
     * 查询人证核验记录
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @POST("androidNew/api/selectPersonCheckRecord")
    fun selectPersonCheckRecord(@Body pageDTO: PersonCardCheckPageDTO?):
            Call<ApiResponse<PageResult<PersonCardCheckModel>?>?>?

    /**
     * 更新车证数据
     * @param carCert 车证信息（JSON格式）
     * @return 响应体
     */
    @POST("androidNew/api/updateCarCert")
    fun updateCarCert(@Body carCert: CarCertificateDTO?): Call<ResponseBody?>?

    /**
     * 查询车证核验记录
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @POST("androidNew/api/selectCarCertCheckRecord")
    fun selectCarCertCheckRecord(@Body pageDTO: CarCertificateCheckPageDTO?):
            Call<ApiResponse<PageResult<CarCertificateCheckModel>?>?>?

    // ==================== 人证绑定相关 ====================
    /**
     * 绑定人证数据
     * @param bindCertDTO 绑定数据
     * @return 响应体，data字段为字符串（如"绑定成功"）
     */
    @POST("androidNew/api/bandCert")
    fun bindCert(@Body bindCertDTO: BindCertDTO?): Call<ApiResponse<String?>?>?

    // ==================== 人脸比对相关 ====================
    /**
     * 人脸比对
     * @param faceMatchParams 图片参数列表（至少需要 2 条记录）
     * @return 人脸比对结果
     */
    @POST("baidu/faceMatch")
    fun faceMatch(@Body faceMatchParams: MutableList<FaceMatchParamDTO?>?): Call<FaceMatchResponseDTO?>?

    /**
     * 更新人员照片
     * @param updatePhotoDTO 更新照片请求数据
     * @return 响应体
     */
    @POST("androidNew/api/updatePhoto")
    fun updatePhoto(@Body updatePhotoDTO: UpdatePhotoDTO?): Call<ApiResponse<Void?>?>?

    // ==================== 新增接口 ====================

    /**
     * 证件照片替换接口
     * @param replacePhotoDTO 照片替换请求数据
     * @return 替换后的照片路径
     */
    @POST("androidNew/api/replacePhoto")
    fun replacePhoto(@Body replacePhotoDTO: ReplacePhotoDTO?): Call<ApiResponse<String?>?>?

    /**
     * 更新（绑定）证件信息接口
     * @param updateCardInfoDTO 更新证件信息请求数据
     * @return 响应体
     */
    @POST("androidNew/api/updateCardInfo")
    fun updateCardInfo(@Body updateCardInfoDTO: UpdateCardInfoDTO?): Call<ApiResponse<Void?>?>?

    /**
     * 设备信息注册接口
     * @param eqpRegisterDTO 设备注册请求数据
     * @return 响应体
     */
    @POST("androidNew/api/eqp/register")
    fun registerEquipment(@Body eqpRegisterDTO: EquipmentRegisterDTO?): Call<ResponseBody?>?
}
