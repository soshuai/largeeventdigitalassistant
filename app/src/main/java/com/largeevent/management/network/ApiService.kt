package com.largeevent.management.network

import com.largeevent.management.network.dto.ActiveInfoVo
import com.largeevent.management.network.dto.ActiveUserBaseVo
import com.largeevent.management.network.dto.ApiResponse
import com.largeevent.management.network.dto.BasicInfoVo
import com.largeevent.management.network.dto.BindCertAo
import com.largeevent.management.network.dto.CarCertificateCheckVo
import com.largeevent.management.network.dto.CarCertificateCheckPageAo
import com.largeevent.management.network.dto.CarCertificateVo
import com.largeevent.management.network.dto.EquipmentRegisterAo
import com.largeevent.management.network.dto.FaceMatchParamAo
import com.largeevent.management.network.dto.FaceMatchResponseVo
import com.largeevent.management.network.dto.MatrixAuthInfoVo
import com.largeevent.management.network.dto.PageVo
import com.largeevent.management.network.dto.PersonCardCheckVo
import com.largeevent.management.network.dto.PersonCardCheckPageAo
import com.largeevent.management.network.dto.ReceiveCheckCarAo
import com.largeevent.management.network.dto.ReceiveCheckPersonAo
import com.largeevent.management.network.dto.ReplacePhotoAo
import com.largeevent.management.network.dto.UpdateCardInfoAo
import com.largeevent.management.network.dto.UpdatePhotoAo
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
     * @return [ApiResponse]；`data` = `List`&lt;[ActiveInfoVo]&gt; 活动列表项
     * @see ActiveInfoVo
     */
    @GET("androidNew/api/getActiveInfo")
    fun getActiveInfo(): Call<ApiResponse<MutableList<ActiveInfoVo?>?>?>?

    /**
     * 获取活动基础信息（场馆/位置/字典/权限矩阵等）。
     *
     * @param activityId 活动ID（必填）
     * @param eqpId 设备ID（必填）
     * @return [ApiResponse]；`data` = [BasicInfoVo]
     * @see BasicInfoVo
     */
    @GET("androidNew/api/getBasicInfo")
    fun getBasicInfo(@Query("activityId") activityId: String?, @Query("eqpId") eqpId: String?): Call<ApiResponse<BasicInfoVo?>?>?

    /**
     * 获取设备通行权限（matrixAuth）。
     *
     * @param activityId 活动ID
     * @param eqpId 设备ID
     * @return [ApiResponse]；`data` = `List`&lt;[MatrixAuthInfoVo]&gt; 权限项列表
     * @see MatrixAuthInfoVo
     */
    @GET("androidNew/api/getMatrixAuthInfoList")
    fun getMatrixAuthInfoList(
        @Query("activityId") activityId: String?,
        @Query("eqpId") eqpId: String?
    ): Call<ApiResponse<MutableList<MatrixAuthInfoVo?>?>?>?

    /**
     * 获取车证设备通行权限（matrixAuth）。
     * 入参、返参与 [getMatrixAuthInfoList] 一致。
     *
     * @param activityId 活动ID
     * @param eqpId 设备ID
     * @return [ApiResponse]；`data` = `List`&lt;[MatrixAuthInfoVo]&gt; 权限项列表
     * @see MatrixAuthInfoVo
     */
    @GET("androidNew/api/getCarMatrixAuthList")
    fun getCarMatrixAuthList(
        @Query("activityId") activityId: String?,
        @Query("eqpId") eqpId: String?
    ): Call<ApiResponse<MutableList<MatrixAuthInfoVo?>?>?>?

    /**
     * 按芯片号查询活动人员/证件。
     *
     * @param activeId 活动ID（必填）
     * @param chipid 芯片号（可选，传则查单人）
     * @return [ApiResponse]；`data` = `List`&lt;[ActiveUserBaseVo]&gt; 人员证件列表
     * @see ActiveUserBaseVo
     */
    @GET("androidNew/api/getActiveUser")
    fun getActiveUser(
        @Query("activeId") activeId: String?,
        @Query("chipid") chipid: String?
    ): Call<ApiResponse<MutableList<ActiveUserBaseVo?>?>?>?

    /**
     * 按证件号码查询证件（仅回显，不上传核验）。
     *
     * @param idNumber 证件号码
     * @param idType 证件类型（如身份证 169）
     * @return [ApiResponse]；`data` = `List`&lt;[ActiveUserBaseVo]&gt;
     * @see ActiveUserBaseVo
     */
    @GET("androidNew/api/getActiveUserByIdNumber")
    fun getActiveUserByIdNumber(
        @Query("idNumber") idNumber: String?,
        @Query("idType") idType: String?
    ): Call<ApiResponse<MutableList<ActiveUserBaseVo?>?>?>?

    /**
     * 按芯片号查询车证详情。
     *
     * @param activeId 活动ID（必填）
     * @param chipid 车证芯片号（IC 物理卡号）
     * @return [ApiResponse]；`data` = `List`&lt;[CarCertificateVo]&gt; 车证列表
     * @see CarCertificateVo
     */
    @GET("androidNew/api/getActiveCarCert")
    fun getActiveCarCert(
        @Query("activeId") activeId: String?,
        @Query("chipid") chipid: String?
    ): Call<ApiResponse<MutableList<CarCertificateVo?>?>?>?

    // ==================== 核验上传 ====================

    /**
     * 上传人证核验结果。
     *
     * @param body 请求体 [ReceiveCheckPersonAo]
     * @return [ResponseBody] 原始响应（无结构化接收类）
     * @see ReceiveCheckPersonAo
     */
    @POST("androidNew/api/receiveCheckPerson")
    fun receiveCheckPerson(@Body body: ReceiveCheckPersonAo?): Call<ResponseBody?>?

    /**
     * 上传车证核验结果。
     * 请求体字段与 [ReceiveCheckPersonAo] 一致。
     *
     * @param body 请求体 [ReceiveCheckCarAo]
     * @return [ResponseBody] 原始响应
     * @see ReceiveCheckCarAo
     */
    @POST("androidNew/api/receiveCheckCar")
    fun receiveCheckCar(@Body body: ReceiveCheckCarAo?): Call<ResponseBody?>?

    /**
     * 分页查询人证核验记录。
     *
     * @param pageDTO 查询条件 [PersonCardCheckPageAo]
     * @return [ApiResponse]；`data` = [PageVo]&lt;[PersonCardCheckVo]&gt;
     * @see PersonCardCheckVo
     * @see PageVo
     */
    @POST("androidNew/api/selectPersonCheckRecord")
    fun selectPersonCheckRecord(
        @Body pageDTO: PersonCardCheckPageAo?
    ): Call<ApiResponse<PageVo<PersonCardCheckVo>?>?>?

    /**
     * 更新车证数据（如绑定车牌）。
     *
     * @param carCert 车证 [CarCertificateVo]
     * @return [ResponseBody] 原始响应（无结构化接收类）
     * @see CarCertificateVo
     */
    @POST("androidNew/api/updateCarCert")
    fun updateCarCert(@Body carCert: CarCertificateVo?): Call<ResponseBody?>?

    /**
     * 分页查询车证核验记录。
     *
     * 请求体字段与 [PersonCardCheckPageAo] 一致。
     *
     * @param pageDTO 查询条件 [CarCertificateCheckPageAo]
     * @return [ApiResponse]；`data` = [PageVo]&lt;[CarCertificateCheckVo]&gt;
     * @see CarCertificateCheckVo
     * @see PageVo
     */
    @POST("androidNew/api/selectCarCertCheckRecord")
    fun selectCarCertCheckRecord(
        @Body pageDTO: CarCertificateCheckPageAo?
    ): Call<ApiResponse<PageVo<CarCertificateCheckVo>?>?>?

    // ==================== 人证绑定相关 ====================

    /**
     * 绑定人证数据。
     *
     * @param bindCertDTO 绑定请求 [BindCertAo]
     * @return [ApiResponse]；`data` = [String]（如「绑定成功」）
     * @see BindCertAo
     */
    @POST("androidNew/api/bandCert")
    fun bindCert(@Body bindCertDTO: BindCertAo?): Call<ApiResponse<String?>?>?

    // ==================== 人脸比对相关 ====================

    /**
     * 人脸比对（百度 / 服务端包装均可）。
     *
     * @param faceMatchParams 图片参数列表（至少 2 条）[FaceMatchParamAo]
     * @return [FaceMatchResponseVo]（非 ApiResponse 包装）
     * @see FaceMatchResponseVo
     */
    @POST("baidu/faceMatch")
    fun faceMatch(
        @Body faceMatchParams: MutableList<FaceMatchParamAo?>?
    ): Call<FaceMatchResponseVo?>?

    /**
     * 更新人员照片。
     *
     * @param updatePhotoDTO 请求 [UpdatePhotoAo]
     * @return [ApiResponse]；`data` = [Void]（无业务体）
     * @see UpdatePhotoAo
     */
    @POST("androidNew/api/updatePhoto")
    fun updatePhoto(@Body updatePhotoDTO: UpdatePhotoAo?): Call<ApiResponse<Void?>?>?

    // ==================== 证件照片 / 绑定 / 设备 ====================

    /**
     * 证件照片替换。
     *
     * @param replacePhotoDTO 请求 [ReplacePhotoAo]
     * @return [ApiResponse]；`data` = [String] 替换后的照片路径
     * @see ReplacePhotoAo
     */
    @POST("androidNew/api/replacePhoto")
    fun replacePhoto(@Body replacePhotoDTO: ReplacePhotoAo?): Call<ApiResponse<String?>?>?

    /**
     * 更新（绑定）证件信息。
     *
     * @param updateCardInfoDTO 请求 [UpdateCardInfoAo]
     * @return [ApiResponse]；`data` = [Void]（无业务体）
     * @see UpdateCardInfoAo
     */
    @POST("androidNew/api/updateCardInfo")
    fun updateCardInfo(@Body updateCardInfoDTO: UpdateCardInfoAo?): Call<ApiResponse<Void?>?>?

    /**
     * 设备注册（人证/车证分类型注册）。
     *
     * @param eqpRegisterDTO 请求 [EquipmentRegisterAo]
     * @return [ResponseBody] 原始响应（无结构化接收类）
     * @see EquipmentRegisterAo
     */
    @POST("androidNew/api/eqp/register")
    fun registerEquipment(@Body eqpRegisterDTO: EquipmentRegisterAo?): Call<ResponseBody?>?
}
