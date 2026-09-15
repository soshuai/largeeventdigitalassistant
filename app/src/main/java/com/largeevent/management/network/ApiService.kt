package com.largeevent.management.network

import com.largeevent.management.network.dto.ActiveInfoDTO
import com.largeevent.management.network.dto.ActiveUserBaseDTO
import com.largeevent.management.network.dto.ApiResponse
import com.largeevent.management.network.dto.BasicInfoDTO
import com.largeevent.management.network.dto.BindCertDTO
import com.largeevent.management.network.dto.CarCertificateCheckModel
import com.largeevent.management.network.dto.CarCertificateCheckPageDTO
import com.largeevent.management.network.dto.CarCertificateDTO
import com.largeevent.management.network.dto.EquipmentRegisterDTO
import com.largeevent.management.network.dto.FaceMatchParamDTO
import com.largeevent.management.network.dto.FaceMatchResponseDTO
import com.largeevent.management.network.dto.MatrixAuthInfoDTO
import com.largeevent.management.network.dto.PageResult
import com.largeevent.management.network.dto.PersonCardCheckModel
import com.largeevent.management.network.dto.PersonCardCheckPageDTO
import com.largeevent.management.network.dto.ReceiveCheckPersonDTO
import com.largeevent.management.network.dto.ReplacePhotoDTO
import com.largeevent.management.network.dto.UpdateCardInfoDTO
import com.largeevent.management.network.dto.UpdatePhotoDTO
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 统一 API 服务接口。
 *
 * 返回约定：
 * - 多数接口外层为 [ApiResponse]，业务数据在 `data` 字段
 * - 仍为 [ResponseBody] 的接口表示后端未提供结构化 body，或调用方按原始字符串处理
 */
interface ApiService {

    // ==================== 基础信息接口 ====================

    /**
     * 获取活动信息列表。
     *
     * @return [ApiResponse]；`data` = `List`&lt;[ActiveInfoDTO]&gt; 活动列表项
     * @see ActiveInfoDTO
     */
    @GET("androidNew/api/getActiveInfo")
    fun getActiveInfo(): Call<ApiResponse<MutableList<ActiveInfoDTO?>?>?>?

    /**
     * 获取活动基础信息（场馆/位置/字典/权限矩阵等）。
     *
     * @param activityId 活动ID（必填）
     * @param eqpId 设备ID（必填）
     * @return [ApiResponse]；`data` = [BasicInfoDTO]
     * @see BasicInfoDTO
     */
    @GET("androidNew/api/getBasicInfo")
    fun getBasicInfo(@Query("activityId") activityId: String?, @Query("eqpId") eqpId: String?): Call<ApiResponse<BasicInfoDTO?>?>?

    /**
     * 获取设备通行权限（matrixAuth）。
     *
     * @param activityId 活动ID
     * @param eqpId 设备ID
     * @return [ApiResponse]；`data` = `List`&lt;[MatrixAuthInfoDTO]&gt; 权限项列表
     * @see MatrixAuthInfoDTO
     */
    @GET("androidNew/api/getMatrixAuthInfoList")
    fun getMatrixAuthInfoList(
        @Query("activityId") activityId: String?,
        @Query("eqpId") eqpId: String?
    ): Call<ApiResponse<MutableList<MatrixAuthInfoDTO?>?>?>?

    /**
     * 按芯片号查询活动人员/证件。
     *
     * @param activeId 活动ID（必填）
     * @param chipid 芯片号（可选，传则查单人）
     * @return [ApiResponse]；`data` = `List`&lt;[ActiveUserBaseDTO]&gt; 人员证件列表
     * @see ActiveUserBaseDTO
     */
    @GET("androidNew/api/getActiveUser")
    fun getActiveUser(
        @Query("activeId") activeId: String?,
        @Query("chipid") chipid: String?
    ): Call<ApiResponse<MutableList<ActiveUserBaseDTO?>?>?>?

    /**
     * 按证件号码查询证件（仅回显，不上传核验）。
     *
     * @param idNumber 证件号码
     * @param idType 证件类型（如身份证 169）
     * @return [ApiResponse]；`data` = `List`&lt;[ActiveUserBaseDTO]&gt;
     * @see ActiveUserBaseDTO
     */
    @GET("androidNew/api/getActiveUserByIdNumber")
    fun getActiveUserByIdNumber(
        @Query("idNumber") idNumber: String?,
        @Query("idType") idType: String?
    ): Call<ApiResponse<MutableList<ActiveUserBaseDTO?>?>?>?

    /**
     * 按芯片号查询车证详情。
     *
     * @param activeId 活动ID（必填）
     * @param chipid 车证芯片号（IC 物理卡号）
     * @return [ApiResponse]；`data` = `List`&lt;[CarCertificateDTO]&gt; 车证列表
     * @see CarCertificateDTO
     */
    @GET("androidNew/api/getActiveCarCert")
    fun getActiveCarCert(
        @Query("activeId") activeId: String?,
        @Query("chipid") chipid: String?
    ): Call<ApiResponse<MutableList<CarCertificateDTO?>?>?>?

    // ==================== 核验上传 ====================

    /**
     * 上传人证核验结果。
     *
     * @param body 请求体 [ReceiveCheckPersonDTO]
     * @return [ResponseBody] 原始响应（无结构化接收类）
     * @see ReceiveCheckPersonDTO
     */
    @POST("androidNew/api/receiveCheckPerson")
    fun receiveCheckPerson(@Body body: ReceiveCheckPersonDTO?): Call<ResponseBody?>?

    /**
     * 上传车证核验结果。
     *
     * @param requestMap 包含 data 字段的 Map
     * @return [ResponseBody] 原始响应（无结构化接收类）
     */
    @POST("androidNew/api/receiveCheckCar")
    fun receiveCheckCar(@Body requestMap: Map<String, String>?): Call<ResponseBody?>?

    /**
     * 分页查询人证核验记录。
     *
     * @param pageDTO 查询条件 [PersonCardCheckPageDTO]
     * @return [ApiResponse]；`data` = [PageResult]&lt;[PersonCardCheckModel]&gt;
     * @see PersonCardCheckModel
     * @see PageResult
     */
    @POST("androidNew/api/selectPersonCheckRecord")
    fun selectPersonCheckRecord(
        @Body pageDTO: PersonCardCheckPageDTO?
    ): Call<ApiResponse<PageResult<PersonCardCheckModel>?>?>?

    /**
     * 更新车证数据（如绑定车牌）。
     *
     * @param carCert 车证 [CarCertificateDTO]
     * @return [ResponseBody] 原始响应（无结构化接收类）
     * @see CarCertificateDTO
     */
    @POST("androidNew/api/updateCarCert")
    fun updateCarCert(@Body carCert: CarCertificateDTO?): Call<ResponseBody?>?

    /**
     * 分页查询车证核验记录。
     *
     * @param pageDTO 查询条件 [CarCertificateCheckPageDTO]
     * @return [ApiResponse]；`data` = [PageResult]&lt;[CarCertificateCheckModel]&gt;
     * @see CarCertificateCheckModel
     * @see PageResult
     */
    @POST("androidNew/api/selectCarCertCheckRecord")
    fun selectCarCertCheckRecord(
        @Body pageDTO: CarCertificateCheckPageDTO?
    ): Call<ApiResponse<PageResult<CarCertificateCheckModel>?>?>?

    // ==================== 人证绑定相关 ====================

    /**
     * 绑定人证数据。
     *
     * @param bindCertDTO 绑定请求 [BindCertDTO]
     * @return [ApiResponse]；`data` = [String]（如「绑定成功」）
     * @see BindCertDTO
     */
    @POST("androidNew/api/bandCert")
    fun bindCert(@Body bindCertDTO: BindCertDTO?): Call<ApiResponse<String?>?>?

    // ==================== 人脸比对相关 ====================

    /**
     * 人脸比对（百度 / 服务端包装均可）。
     *
     * @param faceMatchParams 图片参数列表（至少 2 条）[FaceMatchParamDTO]
     * @return [FaceMatchResponseDTO]（非 ApiResponse 包装）
     * @see FaceMatchResponseDTO
     */
    @POST("baidu/faceMatch")
    fun faceMatch(
        @Body faceMatchParams: MutableList<FaceMatchParamDTO?>?
    ): Call<FaceMatchResponseDTO?>?

    /**
     * 更新人员照片。
     *
     * @param updatePhotoDTO 请求 [UpdatePhotoDTO]
     * @return [ApiResponse]；`data` = [Void]（无业务体）
     * @see UpdatePhotoDTO
     */
    @POST("androidNew/api/updatePhoto")
    fun updatePhoto(@Body updatePhotoDTO: UpdatePhotoDTO?): Call<ApiResponse<Void?>?>?

    // ==================== 证件照片 / 绑定 / 设备 ====================

    /**
     * 证件照片替换。
     *
     * @param replacePhotoDTO 请求 [ReplacePhotoDTO]
     * @return [ApiResponse]；`data` = [String] 替换后的照片路径
     * @see ReplacePhotoDTO
     */
    @POST("androidNew/api/replacePhoto")
    fun replacePhoto(@Body replacePhotoDTO: ReplacePhotoDTO?): Call<ApiResponse<String?>?>?

    /**
     * 更新（绑定）证件信息。
     *
     * @param updateCardInfoDTO 请求 [UpdateCardInfoDTO]
     * @return [ApiResponse]；`data` = [Void]（无业务体）
     * @see UpdateCardInfoDTO
     */
    @POST("androidNew/api/updateCardInfo")
    fun updateCardInfo(@Body updateCardInfoDTO: UpdateCardInfoDTO?): Call<ApiResponse<Void?>?>?

    /**
     * 设备注册（人证/车证分类型注册）。
     *
     * @param eqpRegisterDTO 请求 [EquipmentRegisterDTO]
     * @return [ResponseBody] 原始响应（无结构化接收类）
     * @see EquipmentRegisterDTO
     */
    @POST("androidNew/api/eqp/register")
    fun registerEquipment(@Body eqpRegisterDTO: EquipmentRegisterDTO?): Call<ResponseBody?>?
}
