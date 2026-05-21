package com.largeevent.management.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.largeevent.management.*;
import com.largeevent.management.R;
import com.google.gson.Gson;
import com.largeevent.management.camera.CameraCallback;
import com.largeevent.management.camera.CameraHelper;
import com.largeevent.management.data.ActiveUserParser;
import com.largeevent.management.data.ActiveUserResolver;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.DevicePermissionHelper;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.data.PersonVerificationHelper;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.ActiveUserBaseDTO;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.FaceMatchParamDTO;
import com.largeevent.management.network.dto.FaceMatchResponseDTO;
import com.largeevent.management.network.dto.PersonCardCheckDTO;
import com.largeevent.management.nfc.NfcCallback;
import com.largeevent.management.widget.CommonConfig;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 人证核验页面
 */
public class PersonVerifyFragment extends Fragment implements NfcCallback {

    private static final String TAG = "PersonVerifyFragment";
    private static final String DEFAULT_REQUIRED_ZONE = "";

    private TextView tvNfcStatus;
    private ImageView ivFacePreview;
    private Button btnStartVerify;
    private CameraHelper cameraHelper;
    private InitializationRepository initializationRepository;
    private String currentChipId;
    private String latestPhotoPath;
    private boolean verifying;
    private ActiveUserBaseDTO currentUserDTO;  // 保存当前核验的用户数据

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_person_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvNfcStatus = view.findViewById(R.id.tv_nfc_status);
        ivFacePreview = view.findViewById(R.id.iv_face_preview);
        btnStartVerify = view.findViewById(R.id.btn_start_verify);

        initializationRepository = new InitializationRepository(requireContext());
        cameraHelper = new CameraHelper(this);
        cameraHelper.setCameraCallback(new CameraCallback() {
            @Override
            public void onPhotoTaken(String photoPath) {
                if (!TextUtils.isEmpty(photoPath)) {
                    latestPhotoPath = photoPath;
                    Glide.with(requireContext()).load(photoPath).centerCrop().into(ivFacePreview);
                }
            }

            @Override
            public void onImageSelected(Uri imageUri) {
                Glide.with(requireContext()).load(imageUri).centerCrop().into(ivFacePreview);
            }
        });

        view.findViewById(R.id.card_nfc_area).setOnClickListener(v -> requestNfcStatus());
        view.findViewById(R.id.tv_history).setOnClickListener(v -> openHistoryPage());
        view.findViewById(R.id.card_face_area).setOnClickListener(v -> cameraHelper.takePhoto(true));
        btnStartVerify.setOnClickListener(v -> startVerification());

        // 检查是否已初始化
        checkInitializationStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次页面显示时都检查初始化状态
        checkInitializationStatus();
    }

    /**
     * 检查初始化状态
     */
    private void checkInitializationStatus() {
        boolean isInitialized = isAppInitialized();

        if (!isInitialized) {
            // 未初始化，禁用核验功能
            if (btnStartVerify != null) {
                btnStartVerify.setEnabled(false);
                btnStartVerify.setText("请先初始化");
            }
            if (tvNfcStatus != null) {
                tvNfcStatus.setText("请先到首页完成初始化");
            }
        } else {
            // 已初始化，恢复正常状态
            if (btnStartVerify != null) {
                btnStartVerify.setEnabled(true);
                btnStartVerify.setText("开始核验");
            }
            if (tvNfcStatus != null && tvNfcStatus.getText().toString().contains("请先到首页")) {
                tvNfcStatus.setText("请将证件靠近读卡区域");
            }
        }
    }

    /**
     * 检查应用是否已初始化
     */
    private boolean isAppInitialized() {
        // 检查是否有 activeId
        String activeId = AppPreferences.getLastActiveId(requireContext());
        if (!TextUtils.isEmpty(activeId)) {
            return true;
        }

        // 检查数据库中是否有数据
        try {
            if (initializationRepository != null && initializationRepository.hasData()) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check initialization status", e);
        }

        return false;
    }

    private void requestNfcStatus() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showNfcStatusHint();
        } else {
            Toast.makeText(requireContext(), "请开启NFC功能", Toast.LENGTH_SHORT).show();
        }
    }

    private void openHistoryPage() {
        Intent intent = new Intent(requireContext(), RecordActivity.class);
        intent.putExtra(CommonConfig.EXTRA_IS_CAR, false);
        startActivity(intent);
    }

    @Override
    public void onTagDetected(String tagId, String[] techList) {
        Log.d(TAG, "onTagDetected called: tagId=" + tagId);

        // 检查 Fragment 是否已经 attach 到 Activity
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached to context, ignoring NFC event");
            return;
        }

        Toast.makeText(requireContext(), "检测到nfc设备", Toast.LENGTH_SHORT).show();

        currentChipId = tagId;
        if (tvNfcStatus == null) {
            Log.w(TAG, "tvNfcStatus is null!");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("检测到证件\nID: ").append(tagId);
        if (techList != null && techList.length > 0) {
            sb.append("\n支持技术: ");
            for (String tech : techList) {
                sb.append(tech.substring(tech.lastIndexOf('.') + 1)).append(" ");
            }
        }
        tvNfcStatus.setText(sb.toString());
        Log.d(TAG, "NFC status updated: " + sb.toString());
    }

    private void startVerification() {
        if (true) {
            //TODO 测试 E00401531D5F1031 、E00401531D5F73AE
            currentChipId = "E00401531D5F73AE";
        } else {
            if (verifying) {
                return;
            }
            // 检查是否已初始化
            if (!isAppInitialized()) {
                Toast.makeText(requireContext(), "请先到首页完成初始化", Toast.LENGTH_LONG).show();
                return;
            }
            if (TextUtils.isEmpty(currentChipId)) {
                Toast.makeText(requireContext(), "请先将证件靠近读卡区域", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // 直接调用接口查询证件信息
        setVerifying(true);
        queryUserByChipId(currentChipId);
    }

    /**
     * 调用 android/api/getActiveUser 接口查询证件信息
     */
    private void queryUserByChipId(String chipId) {
        // 获取 activeId，优先从 SharedPreferences 读取，如果没有则从数据库读取
        String activeId = AppPreferences.getLastActiveId(requireContext());
        if (TextUtils.isEmpty(activeId)) {
            // 尝试从数据库中获取 activeId
            try {
                InitializationRepository repository = new InitializationRepository(requireContext());
                BasicInfo basicInfo = repository.getBasicInfo();
                if (basicInfo != null && !basicInfo.getActiveModels().isEmpty()) {
                    activeId = basicInfo.getActiveModels().get(0).id;
                    // 保存到 SharedPreferences 以便下次使用
                    if (!TextUtils.isEmpty(activeId)) {
                        AppPreferences.setLastActiveId(requireContext(), activeId);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get activeId from database", e);
            }
        }

        if (TextUtils.isEmpty(activeId)) {
            setVerifying(false);
            Toast.makeText(requireContext(), "请先完成初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        final String resolvedActiveId = activeId;

        // 使用 ApiService 调用接口，传入 activeId 和 chipId
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<List<ActiveUserBaseDTO>>> call = apiService.getActiveUser(resolvedActiveId, chipId);

        call.enqueue(new Callback<ApiResponse<List<ActiveUserBaseDTO>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<ActiveUserBaseDTO>>> call, @NonNull Response<ApiResponse<List<ActiveUserBaseDTO>>> response) {
                if (!isAdded())
                    return;
                requireActivity().runOnUiThread(() -> {
                    if (!response.isSuccessful() || response.body() == null) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "服务器错误", "查询证件信息失败，状态码：" + response.code(), null), "服务器错误");
                        return;
                    }

                    ApiResponse<List<ActiveUserBaseDTO>> apiResponse = response.body();
                    if (!apiResponse.isSuccess()) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "查询失败", apiResponse.getMessage(), null), "查询失败");
                        return;
                    }

                    List<ActiveUserBaseDTO> userList = apiResponse.getData();
                    if (userList == null || userList.isEmpty()) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "无效证件", "系统中未找到该证件信息", null), "无效证件");
                        return;
                    }

                    String subUnit = AppPreferences.getSelectedSubUnitName(requireContext(), resolvedActiveId);
                    ActiveUserBaseDTO userDTO = ActiveUserResolver.resolve(userList, chipId, subUnit);
                    if (userDTO == null) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "无效证件", "系统中未找到该证件信息", null), "无效证件");
                        return;
                    }

                    ActiveUserParser.normalizeActiveUserDto(userDTO);
                    currentUserDTO = userDTO;

                    // 从 VO 构建 CertificateInfo
                    CertificateInfo info = ActiveUserParser.buildCertificateInfo(userDTO, chipId);

                    // 继续后续验证流程
                    continueVerification(info, userDTO);
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<ActiveUserBaseDTO>>> call, @NonNull Throwable t) {
                if (!isAdded())
                    return;
                requireActivity().runOnUiThread(() -> {
                    setVerifying(false);
                    openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "网络错误", "查询证件信息失败：" + t.getMessage(), null), "网络错误");
                });
            }
        });
    }

    /**
     * 继续后续验证流程（新版通用验证规则 + 证件类型 + 人证合一）。
     */
    private void continueVerification(CertificateInfo info, ActiveUserBaseDTO userDTO) {
        if (info == null || userDTO == null) {
            setVerifying(false);
            openResult(new VerificationResult(VerificationResultType.NOT_DETECTED, "未识读出证件", "证件信息解析失败", null), "未识读出证件");
            return;
        }

        String activeId = AppPreferences.getLastActiveId(requireContext());
        BasicInfo basicInfo = initializationRepository.getBasicInfo();
        DevicePermissionHelper.PermissionSets device = DevicePermissionHelper.resolveDevicePermissions(requireContext(), activeId, basicInfo);

        PersonVerificationHelper.StepResult step = PersonVerificationHelper.runCommonRules(requireContext(), activeId, userDTO, device);

        if (step.step == PersonVerificationHelper.Step.VP_PASS || step.step == PersonVerificationHelper.Step.TP_PASS) {
            setVerifying(false);
            openResult(PersonVerificationHelper.buildVerificationResult(step, info, currentChipId), PersonVerificationHelper.passTitle());
            return;
        }

        if (step.step != PersonVerificationHelper.Step.NEED_FACE_VERIFY) {
            setVerifying(false);
            openResult(PersonVerificationHelper.buildVerificationResult(step, info, currentChipId), step.title != null ? step.title : "");
            return;
        }

        evaluateWithFaceVerification(info, userDTO);
    }

    /**
     * MP 人证合一：通道/闸机强制；手持有拍照则比对。
     */
    private void evaluateWithFaceVerification(CertificateInfo info, ActiveUserBaseDTO userDTO) {
        String activeId = AppPreferences.getLastActiveId(requireContext());
        boolean hasPhoto = !TextUtils.isEmpty(latestPhotoPath);

        if (!PersonVerificationHelper.isFaceVerifyRequired(requireContext(), activeId, hasPhoto)) {
            Log.d(TAG, "跳过人证合一, hasPhoto=" + hasPhoto);
            finishPass(info);
            return;
        }

        if (PersonVerificationHelper.isFaceVerifyMandatory(requireContext(), activeId) && !hasPhoto) {
            setVerifying(false);
            openResult(new VerificationResult(VerificationResultType.FACE_MISMATCH, PersonVerificationHelper.faceMismatchTitle(requireContext(), activeId), "请先拍照进行人证比对", info), "请检查证件");
            return;
        }

        // 检查是否有头像 URL
        String photoUrl = userDTO.photo;
        if (TextUtils.isEmpty(photoUrl)) {
            setVerifying(false);
            openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "无法比对", "证件没有头像信息，无法进行人脸比对", info), "证件缺少头像");
            return;
        }

        // 转换拍摄的照片为 Base64
        String base64Image = convertImageToBase64(latestPhotoPath);
        if (TextUtils.isEmpty(base64Image)) {
            setVerifying(false);
            Toast.makeText(requireContext(), "照片处理失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 异步下载并转换证件照片为 Base64
        new Thread(() -> {
            try {
                String photoBase64 = downloadImageAndConvertToBase64(photoUrl);

                if (TextUtils.isEmpty(photoBase64)) {
                    requireActivity().runOnUiThread(() -> {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "无法比对", "证件照片下载失败", info), "证件照片下载失败");
                    });
                    return;
                }

                // 在主线程调用人脸比对接口
                requireActivity().runOnUiThread(() -> {
                    performFaceMatch(base64Image, photoBase64, info, userDTO);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setVerifying(false);
                    openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "无法比对", "证件照片处理失败：" + e.getMessage(), info), "证件照片处理失败");
                    Log.e(TAG, "Download and convert photo failed", e);
                });
            }
        }).start();
    }

    /**
     * 执行人脸比对
     */
    private void performFaceMatch(String base64Image1, String base64Image2, CertificateInfo info, ActiveUserBaseDTO userDTO) {
        // 构建人脸比对参数（两张图都使用 Base64）
        List<FaceMatchParamDTO> faceMatchParams = new ArrayList<>();
        // 第一张图：Base64 拍摄的照片
        faceMatchParams.add(new FaceMatchParamDTO(base64Image1, "BASE64", "LIVE"));
        // 第二张图：Base64 证件的头像（从 URL 下载转换而来）
        faceMatchParams.add(new FaceMatchParamDTO(base64Image2, "BASE64", "LIVE"));

        // 调用人脸比对接口
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<FaceMatchResponseDTO> call = apiService.faceMatch(faceMatchParams);

        call.enqueue(new Callback<FaceMatchResponseDTO>() {
            @Override
            public void onResponse(@NonNull Call<FaceMatchResponseDTO> call, @NonNull Response<FaceMatchResponseDTO> response) {
                if (!isAdded())
                    return;
                requireActivity().runOnUiThread(() -> {
                    if (!response.isSuccessful() || response.body() == null) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "人脸比对失败", "服务器错误，状态码：" + response.code(), info), "人脸比对失败");
                        return;
                    }

                    FaceMatchResponseDTO faceMatchResponse = response.body();

                    // 检查接口调用是否成功（code = 200）
                    if (!faceMatchResponse.isSuccess()) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "人脸比对失败", faceMatchResponse.getErrorMessage(), info), "人脸比对失败");
                        return;
                    }

                    // 检查百度 API 返回的错误码
                    if (faceMatchResponse.data != null && faceMatchResponse.data.errorCode != 0) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "人脸比对失败", faceMatchResponse.data.errorMsg, info), "人脸比对失败");
                        return;
                    }

                    // 检查 score 分数
                    FaceMatchResponseDTO.Result result = faceMatchResponse.data != null ? faceMatchResponse.data.result : null;
                    if (result == null) {
                        setVerifying(false);
                        openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "人脸比对失败", "", info), "人脸比对失败");
                        return;
                    }

                    double score = result.score;
                    Log.d(TAG, "Face match score: " + score);

                    // score >= 80 才算成功
                    if (score < 80) {
                        setVerifying(false);
                        String activeId = AppPreferences.getLastActiveId(requireContext());
                        openResult(new VerificationResult(VerificationResultType.FACE_MISMATCH, PersonVerificationHelper.faceMismatchTitle(requireContext(), activeId), PersonVerificationHelper.faceMismatchDescription(requireContext(), activeId), info), "请检查证件");
                        return;
                    }

                    Log.d(TAG, "Face match passed, score: " + score);
                    finishPass(info);
                });
            }

            @Override
            public void onFailure(@NonNull Call<FaceMatchResponseDTO> call, @NonNull Throwable t) {
                if (!isAdded())
                    return;
                requireActivity().runOnUiThread(() -> {
                    setVerifying(false);
                    openResult(new VerificationResult(VerificationResultType.INVALID_CERT, "网络错误", "人脸比对失败：" + t.getMessage(), info), "人脸比对网络错误");
                    Log.e(TAG, "Face match network error", t);
                });
            }
        });
    }

    private void finishPass(CertificateInfo info) {
        setVerifying(false);
        openResult(new VerificationResult(VerificationResultType.PASS, PersonVerificationHelper.passTitle(), "", info), PersonVerificationHelper.passTitle());
    }

    private void openResult(VerificationResult result, String recordDesc) {
        setVerifying(false);

        // 上传核验结果到后台
        uploadVerificationResult(result);

        Intent intent = new Intent(requireContext(), CarAndPersonDetailActivity.class);
        intent.putExtra(CarAndPersonDetailActivity.EXTRA_RESULT, result);
        intent.putExtra(CommonConfig.EXTRA_IS_CAR, false);
        startActivity(intent);
    }

    /**
     * 上传核验结果到后台
     */
    private void uploadVerificationResult(VerificationResult result) {
        if (currentUserDTO == null) {
            Log.w(TAG, "currentUserDTO is null, skip upload");
            return;
        }

        try {
            // 构建上传数据
            PersonCardCheckDTO checkData = new PersonCardCheckDTO();
            checkData.idCard = currentUserDTO.idNumber;
            checkData.activeCode = currentUserDTO.activeProfile;
            checkData.activeId = currentUserDTO.activeId;
            checkData.userId = currentUserDTO.userId;
            checkData.personType = currentUserDTO.activeProfile;  // 人员类型
            checkData.personName = currentUserDTO.chineseName;
            checkData.sex = currentUserDTO.gender;
            checkData.unitName = currentUserDTO.organization;
            checkData.phone = currentUserDTO.phoneNumber;
            checkData.personPhoto = currentUserDTO.photo;
            checkData.subAppTypeCode = currentUserDTO.subAppTypeCode;
            checkData.tagNo = currentUserDTO.tagNo1;
            checkData.session = "";  // 场次（如果有的话从其他地方获取）
            checkData.deviceName = android.os.Build.MODEL;  // 设备名称
            checkData.passRuleCode = currentUserDTO.passRuleCode;

            // 组装通行位置代码（场馆code + 区域code），来自设置页当前活动的权限选择
            try {
                String currentActiveId = AppPreferences.getLastActiveId(requireContext());
                String locationId = AppPreferences.getSelectedLocationId(requireContext(), currentActiveId);
                checkData.positionId = locationId;  // 位置（如果有的话从设置中获取）

                Set<String> venueNames = AppPreferences.getSelectedVenuePermissions(requireContext(), currentActiveId);
                Set<String> areaNames = AppPreferences.getSelectedAreaPermissions(requireContext(), currentActiveId);
                Set<String> codeSet = new java.util.HashSet<>();

                BasicInfo basicInfo2 = initializationRepository.getBasicInfo();
                if (basicInfo2 != null) {
                    // 场馆名称 -> venueCode
                    java.util.List<BasicInfo.ActiveVenueModel> venues = basicInfo2.getActiveVenues();
                    if (venues != null && venueNames != null) {
                        for (String vn : venueNames) {
                            if (TextUtils.isEmpty(vn))
                                continue;
                            for (BasicInfo.ActiveVenueModel v : venues) {
                                if (v != null && vn.equals(v.venueName) && !TextUtils.isEmpty(v.venueCode)) {
                                    codeSet.add(v.venueCode);
                                    break;
                                }
                            }
                        }
                    }
                    // 区域名称 -> positionCode
                    java.util.List<BasicInfo.PositionModel> positions2 = basicInfo2.getPositions();
                    if (positions2 != null && areaNames != null) {
                        for (String an : areaNames) {
                            if (TextUtils.isEmpty(an))
                                continue;
                            for (BasicInfo.PositionModel p : positions2) {
                                if (p != null && an.equals(p.name) && !TextUtils.isEmpty(p.positionCode)) {
                                    codeSet.add(p.positionCode);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (codeSet.isEmpty()) {
                    checkData.passPositionCode = "";
                } else {
                    checkData.passPositionCode = TextUtils.join(",", codeSet);
                }
            } catch (Exception ignore) {
                checkData.passPositionCode = "";
            }
            checkData.certNumber = currentUserDTO.registerNumber;

            // 设置通行时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            checkData.passTime = sdf.format(new Date());

            // 根据核验结果设置方向、错误信息和校验状态
            if (result.type == VerificationResultType.PASS) {
                checkData.direction = "进入";
                checkData.errorMsg = result.title + ": " + result.description;  // 核验成功信息
                checkData.checkStatus = 1;  // 校验成功传1
            } else {
                checkData.direction = "拒绝";  // 核验失败记录为拒绝
                checkData.errorMsg = result.title + ": " + result.description;  // 核验失败原因
                checkData.checkStatus = 0;  // 校验失败传0
            }
            // 设置验证状态码（VerifyStatus）和验证方向
            checkData.verifyStatus = result.type.code;  // 使用状态码（1-未识读, 2-无效, 3-黑名单, 4-注销, 5-未激活, 6-无权, 8-通过, 9-人证不一, 10-未绑定）
            checkData.verifyDirection = checkData.direction;  // 验证方向：进入/拒绝

            // 如果有拍照,上传拍照的base64图片(无论成功还是失败都上传)
            if (!TextUtils.isEmpty(latestPhotoPath)) {
                try {
                    String checkImgBase64 = convertImageToBase64(latestPhotoPath);
                    if (!TextUtils.isEmpty(checkImgBase64)) {
                        checkData.checkImg = checkImgBase64;
                        Log.d(TAG, "添加拍照图片,Base64长度: " + checkImgBase64.length());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "转换拍照图片为Base64失败: " + e.getMessage());
                }
            }

            // 获取活动名称和证件有效时间信息
            try {
                // 获取活动名称
                BasicInfo basicInfo = initializationRepository.getBasicInfo();
                if (basicInfo != null && !basicInfo.getActiveModels().isEmpty()) {
                    checkData.activeName = basicInfo.getActiveModels().get(0).activeName;
                }

                // 获取证件有效时间
                checkData.startTime = currentUserDTO.validBegin;  // 证件有效期开始时间
                checkData.endTime = currentUserDTO.validEnd;  // 证件有效期结束时间
            } catch (Exception e) {
                Log.w(TAG, "获取活动名称或证件有效时间失败: " + e.getMessage());
            }

            // 构建数组（接口要求数组格式）
            List<PersonCardCheckDTO> dataList = new ArrayList<>();
            dataList.add(checkData);

            // 转换为JSON
            Gson gson = new Gson();
            String jsonData = gson.toJson(dataList);

            // 将JSON字符串封装到Map中，作为data参数
            Map<String, String> requestMap = new HashMap<>();
            requestMap.put("data", jsonData);

            // 调用上传接口
            ApiService apiService = NetworkManager.getInstance().getApiService();
            Call<ResponseBody> call = apiService.receiveCheckPerson(requestMap);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "核验结果上传成功");
                    } else {
                        Log.e(TAG, "核验结果上传失败: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Log.e(TAG, "核验结果上传失败: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "构建上传数据失败: " + e.getMessage());
        }
    }

    private boolean hasRequiredAreaPermission(@Nullable CertificateInfo info) {
        if (info == null) {
            Log.w(TAG, "权限校验失败: CertificateInfo为null");
            return false;
        }

        try {
            String currentActiveId = AppPreferences.getLastActiveId(requireContext());
            BasicInfo basicInfo = initializationRepository.getBasicInfo();
            com.largeevent.management.data.DevicePermissionHelper.PermissionSets device = com.largeevent.management.data.DevicePermissionHelper.resolveDevicePermissions(requireContext(), currentActiveId, basicInfo);

            if (device.isEmpty()) {
                Log.i(TAG, "权限校验通过: 设备未配置场馆/区域/分区权限要求");
                return true;
            }

            Log.d(TAG, "开始权限校验");
            Log.d(TAG, "设备场馆权限(code): " + device.venueCodes);
            Log.d(TAG, "设备区域权限(code, venuePartition): " + device.zoneCodes);
            Log.d(TAG, "设备分区权限(code, venueArea): " + device.partitionCodes);
            Log.d(TAG, "证件场馆(venuePrivileges): " + info.venuePrivileges);
            Log.d(TAG, "证件分区(areaPrivileges): " + info.areaPrivileges);
            Log.d(TAG, "证件区域(zonePrivileges): " + info.zonePrivileges);

            boolean finalResult = DevicePermissionHelper.certificateMatchesDevice(info.venuePrivileges, info.areaPrivileges, info.zonePrivileges, info.sportPrivileges, device, true);

            Log.i(TAG, "最终权限校验结果: " + (finalResult ? "通过" : "不通过"));
            return finalResult;

        } catch (Exception e) {
            Log.e(TAG, "权限校验异常: " + e.getMessage(), e);
            return false;
        }
    }

    private void setVerifying(boolean verifying) {
        this.verifying = verifying;
        if (btnStartVerify != null) {
            btnStartVerify.setEnabled(!verifying);
            btnStartVerify.setText(verifying ? "核验中..." : "开始核验");
        }
    }

    /**
     * 将图片转换为 Base64 字符串
     */
    private String convertImageToBase64(String imagePath) {
        try {
            FileInputStream fis = new FileInputStream(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            if (bitmap == null) {
                return null;
            }

            // 压缩图片
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();

            // 转换为 Base64（不包含 data:image/jpeg;base64, 前缀）
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Convert image to base64 failed", e);
            return null;
        }
    }

    /**
     * 下载网络图片并转换为 Base64
     */
    private String downloadImageAndConvertToBase64(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();

            java.io.InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();

            if (bitmap == null) {
                return null;
            }

            // 压缩图片
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();

            // 转换为 Base64
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Download and convert image to base64 failed", e);
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (cameraHelper != null) {
            if (requestCode == CameraHelper.REQUEST_CODE_TAKE_PHOTO) {
                cameraHelper.handleTakePhotoResult(resultCode);
            } else if (requestCode == CameraHelper.REQUEST_CODE_SELECT_IMAGE) {
                cameraHelper.handleSelectImageResult(resultCode, data);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (initializationRepository != null) {
            initializationRepository.close();
            initializationRepository = null;
        }
    }
}

