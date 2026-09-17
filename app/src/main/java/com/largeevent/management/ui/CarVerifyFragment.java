package com.largeevent.management.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.handheld.uhfr.UHFRManager;
import com.largeevent.management.R;
import com.largeevent.management.CarAndPersonDetailActivity;
import com.largeevent.management.RecordActivity;
import com.largeevent.management.data.*;
import com.largeevent.management.model.*;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.CarCertificateDTO;
import com.largeevent.management.network.dto.ReceiveCheckCarDTO;
import com.largeevent.management.nfc.NfcCallback;
import com.largeevent.management.network.*;
import com.largeevent.management.widget.CommonConfig;
import com.uhf.api.cls.Reader;
import com.largeevent.management.model.BasicInfo.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 车证核验
 */
public class CarVerifyFragment extends BaseFragment implements NfcCallback {

    private static final String TAG = "VehicleVerifyFragment";
    private static final String DEFAULT_REQUIRED_ZONE = "车辆区";
    /** 手持机手柄键广播（实测：extras keyCode=137, keydown=true/false） */
    private static final String ACTION_FUN_KEY = "android.rfid.FUN_KEY";
    private static final int HANDLE_KEY_CODE = 137;
    /** 手柄连发/长按防抖，与点按感应区一样只触发一次盘点 */
    private static final long HANDLE_DEBOUNCE_MS = 600L;

    private TextView tvVehicleStatus;
    private Button btnVerify;
    private InitializationRepository initializationRepository;
    private String currentChipId;
    private CarCertificateDTO currentCarDTO;  // 当前车证DTO
    private boolean verifying;
    private final List<String> currentEpcList = new ArrayList<>();
    private boolean isReading = false;
    private UHFRManager uhfManager;
    private ExecutorService inventoryExecutor;
    private Handler mainHandler;
    private BroadcastReceiver funKeyReceiver;
    private long lastHandleTriggerElapsedMs = 0L;

    private final boolean test = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicle_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvVehicleStatus = view.findViewById(R.id.tv_vehicle_status);
        btnVerify = view.findViewById(R.id.btn_vehicle_verify);
        initializationRepository = new InitializationRepository(requireContext());
        inventoryExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        view.findViewById(R.id.card_vehicle_rfid).setOnClickListener(v -> {
            Log.i(TAG, "UI click card_vehicle_rfid → startUhfInventory()");
            // 与手柄同一入口，保证盘点逻辑完全一致
            startUhfInventory("ui_card_click");
        });
        view.findViewById(R.id.tv_vehicle_history).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), RecordActivity.class);
            intent.putExtra(CommonConfig.EXTRA_IS_CAR, true);
            startActivity(intent);
        });
        btnVerify.setOnClickListener(v -> startVerification());

        // 检查是否已初始化
        checkInitializationStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次页面显示时都检查初始化状态
        checkInitializationStatus();
        registerFunKeyReceiver();
    }

    @Override
    public void onPause() {
        unregisterFunKeyReceiver();
        super.onPause();
    }

    private void registerFunKeyReceiver() {
        if (funKeyReceiver != null || getContext() == null) {
            return;
        }
        funKeyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !ACTION_FUN_KEY.equals(intent.getAction())) {
                    return;
                }
                int keyCode = intent.getIntExtra("keyCode", -1);
                boolean keyDown = intent.getBooleanExtra("keydown", false);
                Log.i(TAG, "FUN_KEY received keyCode=" + keyCode + " keydown=" + keyDown);
                // 仅接受实测手柄键；松开忽略。切到主线程，与点击感应区同一套 startUhfInventory
                if (!keyDown || keyCode != HANDLE_KEY_CODE) {
                    return;
                }
                if (mainHandler != null) {
                    mainHandler.post(() -> startUhfInventoryFromHandle());
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_FUN_KEY);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(funKeyReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                requireContext().registerReceiver(funKeyReceiver, filter);
            }
            Log.i(TAG, "FUN_KEY receiver registered");
        } catch (Exception e) {
            Log.w(TAG, "register FUN_KEY receiver failed", e);
            funKeyReceiver = null;
        }
    }

    private void unregisterFunKeyReceiver() {
        if (funKeyReceiver == null) {
            return;
        }
        Context context = getContext();
        if (context != null) {
            try {
                context.unregisterReceiver(funKeyReceiver);
                Log.i(TAG, "FUN_KEY receiver unregistered");
            } catch (Exception e) {
                Log.w(TAG, "unregister FUN_KEY receiver failed", e);
            }
        }
        funKeyReceiver = null;
    }

    /**
     * 手柄按下：防抖后走与点击感应区完全相同的盘点方法。
     */
    private void startUhfInventoryFromHandle() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastHandleTriggerElapsedMs < HANDLE_DEBOUNCE_MS) {
            Log.d(TAG, "FUN_KEY debounced, ignore duplicate trigger");
            return;
        }
        lastHandleTriggerElapsedMs = now;
        startUhfInventory("handle_fun_key");
    }

    /**
     * 检查初始化状态
     */
    private void checkInitializationStatus() {
        boolean isInitialized = isAppInitialized();

        if (!isInitialized) {
            // 未初始化，禁用核验功能
            if (btnVerify != null) {
                btnVerify.setEnabled(false);
                btnVerify.setText("请先初始化");
            }
            if (tvVehicleStatus != null) {
                tvVehicleStatus.setText("请先到首页完成初始化");
            }
        } else {
            // 已初始化，恢复正常状态
            if (btnVerify != null) {
                btnVerify.setEnabled(true);
                btnVerify.setText("开始核验");
            }
            if (tvVehicleStatus != null && tvVehicleStatus.getText().toString().contains("请先到首页")) {
                tvVehicleStatus.setText("请点击上方按钮或按手柄读取车证");
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

    private void startUhfInventory() {
        startUhfInventory("unspecified");
    }

    /**
     * UHF 盘点唯一入口：界面点击感应区、手柄 FUN_KEY 都走这里，行为一致。
     */
    private void startUhfInventory(String source) {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "startUhfInventory aborted, fragment not attached, source=" + source);
            return;
        }
        if (inventoryExecutor == null || inventoryExecutor.isShutdown()) {
            Log.w(TAG, "startUhfInventory aborted, executor unavailable, source=" + source);
            return;
        }
        Log.i(TAG, "startUhfInventory source=" + source + " isReading=" + isReading);

        if (!isAppInitialized()) {
            Toast.makeText(requireContext(), "请先到首页完成初始化", Toast.LENGTH_LONG).show();
            return;
        }

        if (isReading) {
            // 手柄连发时静默忽略，避免刷 Toast；手动点按仍提示
            if ("ui_card_click".equals(source)) {
                Toast.makeText(requireContext(), "正在读取中，请稍候...", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "already reading, ignore source=" + source);
            }
            return;
        }
        isReading = true;
        if (tvVehicleStatus != null) {
            tvVehicleStatus.setText("正在读取车证...");
        }
        inventoryExecutor.execute(() -> {
            List<String> newEpcList = new ArrayList<>();
            if (test) {
                //E2827802000000003667B210 E28011B0A503007A28B8EE2C
                newEpcList.add("E28011B0A503007A28B8EE2C");
            } else {
                if (!ensureUhfReady()) {
                    postInventoryResult(new ArrayList<>(), "UHF模块初始化失败");
                    return;
                }
                try {
                    List<Reader.TAGINFO> tagInfos = uhfManager.tagInventoryRealTime();
                    if (tagInfos == null || tagInfos.isEmpty()) {
                        tagInfos = uhfManager.tagInventoryByTimer((short) 50);
                    }
                    if (tagInfos != null) {
                        for (Reader.TAGINFO info : tagInfos) {
                            String epc = bytesToHex(info.EpcId);
                            if (!TextUtils.isEmpty(epc) && !newEpcList.contains(epc)
                                    && (epc.contains("e") || epc.contains("E"))) {
                                newEpcList.add(epc);
                            }
                        }
                    }
                } catch (Exception e) {
                    postInventoryResult(new ArrayList<>(), "读取失败：" + e.getMessage());
                    return;
                }
            }
            postInventoryResult(newEpcList, newEpcList.isEmpty() ? "未读取到车证信息" : null);
        });
    }

    private void postInventoryResult(List<String> epcs, @Nullable String errorMsg) {
        if (mainHandler == null) {
            return;
        }
        mainHandler.post(() -> {
            isReading = false;
            if (!isAdded() || getContext() == null) {
                return;
            }
            currentEpcList.clear();
            currentEpcList.addAll(epcs);
            if (epcs.isEmpty()) {
                if (tvVehicleStatus != null) {
                    tvVehicleStatus.setText("未读取到车证");
                }
                if (!TextUtils.isEmpty(errorMsg)) {
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            currentChipId = epcs.get(0);
            if (tvVehicleStatus != null) {
                tvVehicleStatus.setText("已读取芯片：" + currentChipId);
            }
            if (!TextUtils.isEmpty(errorMsg)) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean ensureUhfReady() {
        if (uhfManager != null) {
            return true;
        }
        try {
            uhfManager = UHFRManager.getInstance();
        } catch (Exception e) {
            uhfManager = null;
        }
        if (uhfManager == null) {
            return false;
        }
        try {
            Reader.READER_ERR err = uhfManager.setPower(30, 30);
            if (err != Reader.READER_ERR.MT_OK_ERR) {
                return false;
            }
            try {
                uhfManager.setRegion(Reader.Region_Conf.RG_PRC);
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String bytesToHex(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 获取最近一次读取到的 EPC 列表，便于其它功能使用
     */
    @NonNull
    public List<String> getCurrentEpcList() {
        return new ArrayList<>(currentEpcList);
    }

    private void startVerification() {
        if (verifying)
            return;

        // 检查是否已初始化
        if (!isAppInitialized()) {
            Toast.makeText(requireContext(), "请先到首页完成初始化", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(currentChipId)) {
            Toast.makeText(requireContext(), "请先读取车证", Toast.LENGTH_SHORT).show();
            return;
        }

        // 调用接口查询车证信息
        setVerifying(true);
        queryVehicleCertByChipId(currentChipId);
    }

    /**
     * 调用 /android/getActiveCarCert 接口查询车证信息
     */
    private void queryVehicleCertByChipId(String chipId) {
        // 获取 activeId，优先从 SharedPreferences 读取（设置页/Home 初始化时已保存活动ID）
        String activeId = AppPreferences.getLastActiveId(requireContext());

        if (TextUtils.isEmpty(activeId)) {
            setVerifying(false);
            VerificationResult invalid = new VerificationResult(
                    VerificationResultType.INVALID_CERT, "未初始化", "请先完成初始化", null, false, null, true);
            uploadCheckRecord(invalid);
            recordAndOpen(invalid);
            return;
        }

        // 使用 ApiService 调用接口，传入 activeId 和 chipId
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<List<CarCertificateDTO>>> call = apiService.getActiveCarCert(activeId, chipId);

        call.enqueue(new Callback<ApiResponse<List<CarCertificateDTO>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<CarCertificateDTO>>> call, @NonNull Response<ApiResponse<List<CarCertificateDTO>>> response) {
                if (!isAdded())
                    return;
                requireActivity().runOnUiThread(() -> {
                    if (!response.isSuccessful() || response.body() == null) {
                        setVerifying(false);
                        VerificationResult invalid = new VerificationResult(
                                VerificationResultType.INVALID_CERT, "服务器错误",
                                "查询车证信息失败，状态码：" + response.code(), null, false, null, true);
                        uploadCheckRecord(invalid);
                        recordAndOpen(invalid);
                        return;
                    }

                    ApiResponse<List<CarCertificateDTO>> apiResponse = response.body();
                    if (!apiResponse.isSuccess()) {
                        setVerifying(false);
                        VerificationResult invalid = new VerificationResult(
                                VerificationResultType.INVALID_CERT, "查询失败",
                                apiResponse.getMessage(), null, false, null, true);
                        uploadCheckRecord(invalid);
                        recordAndOpen(invalid);
                        return;
                    }

                    List<CarCertificateDTO> carList = apiResponse.getData();
                    if (carList == null || carList.isEmpty()) {
                        setVerifying(false);
                        VerificationResult invalid = new VerificationResult(
                                VerificationResultType.INVALID_CERT, "无效证件",
                                "系统中未找到该车证信息", null, false, null, true);
                        uploadCheckRecord(invalid);
                        recordAndOpen(invalid);
                        return;
                    }

                    // 获取第一条数据
                    CarCertificateDTO carDTO = carList.get(0);
                    currentCarDTO = carDTO;  // 保存当前DTO
                    CertificateInfo info = CarCertificateParser.buildCertificateInfo(carDTO, chipId);
                    // 检查车牌号是否为空
                    if (TextUtils.isEmpty(CarCertificateParser.resolvePlate(carDTO))) {
                        setVerifying(false);
                        VerificationResult bindResult = new VerificationResult(
                                VerificationResultType.BIND_REQUIRED, "车牌未绑定",
                                "车证信息匹配，车牌已自动绑定", info, true, chipId, true);
                        uploadCheckRecord(bindResult);
                        recordAndOpen(bindResult, carDTO);
                        return;
                    }
                    // 继续后续验证流程
                    continueVerification(info);
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<CarCertificateDTO>>> call, @NonNull Throwable t) {
                if (!isAdded())
                    return;
                requireActivity().runOnUiThread(() -> {
                    setVerifying(false);
                    VerificationResult invalid = new VerificationResult(
                            VerificationResultType.INVALID_CERT, "网络错误",
                            "查询车证信息失败：" + t.getMessage(), null, false, null, true);
                    uploadCheckRecord(invalid);
                    recordAndOpen(invalid);
                });
            }
        });
    }

    /**
     * 继续后续验证流程（权限校验）
     */
    private void continueVerification(CertificateInfo info) {
        if (info == null) {
            setVerifying(false);
            VerificationResult invalid = new VerificationResult(
                    VerificationResultType.INVALID_CERT, "无效证件", "车证信息解析失败", null, false, null, true);
            uploadCheckRecord(invalid);
            recordAndOpen(invalid);
            return;
        }

        // 1. 验证注销状态（eventStatus=6 表示已注销）
        if (currentCarDTO != null && !TextUtils.isEmpty(currentCarDTO.eventStatus)) {
            if ("6".equals(currentCarDTO.eventStatus)) {
                setVerifying(false);
                VerificationResult canceled = new VerificationResult(
                        VerificationResultType.CANCELED, "证件已注销", "该车证已被注销，禁止通行", info, false, null, true);
                uploadCheckRecord(canceled);
                recordAndOpen(canceled);
                return;
            }
        }

        // 2. 验证证件是否挂失
        if (currentCarDTO != null && !TextUtils.isEmpty(currentCarDTO.isLost)) {
            if ("1".equals(currentCarDTO.isLost)) {
                setVerifying(false);
                VerificationResult lost = new VerificationResult(
                        VerificationResultType.BLACKLIST, "限制通行", "该车证已被挂失，禁止通行", info, false, null, true);
                uploadCheckRecord(lost);
                recordAndOpen(lost);
                return;
            }
        }

        // 3. 验证激活状态（eventStatus!=5 表示未激活）
        if (currentCarDTO != null && !TextUtils.isEmpty(currentCarDTO.eventStatus)) {
            if (!"5".equals(currentCarDTO.eventStatus)&&!"4".equals(currentCarDTO.eventStatus)) {
                setVerifying(false);
                VerificationResult notActivated = new VerificationResult(
                        VerificationResultType.NOT_ACTIVATED, "证件未激活", "该车证尚未激活，禁止通行", info, false, null, true);
                uploadCheckRecord(notActivated);
                recordAndOpen(notActivated);
                return;
            }
        }

        // 4. 验证有效期（startTime 和 endTime）
        if (currentCarDTO != null) {
            if (!isValidPeriod(currentCarDTO.startTime, currentCarDTO.endTime)) {
                setVerifying(false);
                VerificationResult expired = new VerificationResult(
                        VerificationResultType.EXPIRED, "证件已过期", "该车证不在有效期内", info, false, null, true);
                uploadCheckRecord(expired);
                recordAndOpen(expired);
                return;
            }
        }

        // 3. 检查是否需要绑定（车证一般不需要绑定，这里保留逻辑以防万一）
        boolean bound = info.isBound() || VehicleBindingStore.isBound(info.chipId);
        if (info.isNeedBinding() && !bound) {
            setVerifying(false);
            VerificationResult bindResult = new VerificationResult(
                    VerificationResultType.BIND_REQUIRED, "车牌未绑定", "请绑定车牌后再尝试", info, true, info.chipId, true);
            uploadCheckRecord(bindResult);
            recordAndOpen(bindResult);
            return;
        }

        // 4. 检查权限（优先使用 accessAuthority，其次使用 area）
        String requiredDesc = getRequiredPermissionSummary(currentCarDTO);
        if (StrUtil.isNotEmpty(requiredDesc)) {
            setVerifying(false);
            VerificationResult denied = new VerificationResult(VerificationResultType.PERMISSION_DENIED,
                    "无权通行", requiredDesc, info, false, null, true);
            uploadCheckRecord(denied);
            recordAndOpen(denied, currentCarDTO);
            return;
        }

        // 核验通过
        setVerifying(false);

        String plateNumber = resolvePlateNumber(info);
        String plateDisplay = TextUtils.isEmpty(plateNumber) ? "—" : plateNumber;

        VerificationResult pass = new VerificationResult(
                VerificationResultType.PASS, "核验通过", plateDisplay, info, false, null, true);
        uploadCheckRecord(pass);
        recordAndOpen(pass);
    }

    @Nullable
    private String resolvePlateNumber(@Nullable CertificateInfo info) {
        if (info != null && !TextUtils.isEmpty(info.cardSerial)) {
            return info.cardSerial.trim();
        }
        String plate = CarCertificateParser.resolvePlate(currentCarDTO);
        return plate != null ? plate : "";
    }

    /**
     * 上传车证核验记录（入参结构与人证 receiveCheckPerson 一致）
     */
    private void uploadCheckRecord(VerificationResult result) {
        try {
            BasicInfo basicInfo = initializationRepository.getBasicInfo();
            ReceiveCheckCarDTO body = ReceiveCheckCarBuilder.build(
                    requireContext(),
                    result,
                    currentCarDTO,
                    currentChipId,
                    basicInfo);
            Log.d(TAG, "上传车证核验记录: " + new Gson().toJson(body));
            ApiService apiService = NetworkManager.getInstance().getApiService();
            Call<okhttp3.ResponseBody> call = apiService.receiveCheckCar(body);
            if (call == null) {
                Log.e(TAG, "车证核验记录上传失败: Call 为空");
                return;
            }
            call.enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "车证核验记录上传成功");
                    } else {
                        Log.e(TAG, "车证核验记录上传失败: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                    Log.e(TAG, "车证核验记录上传失败", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "构造车证核验记录失败", e);
        }
    }

    /**
     * 验证有效期
     * 如果开始时间和结束时间为空，证件就是长期有效
     */
    private boolean isValidPeriod(String startTime, String endTime) {
        // 如果开始时间和结束时间都为空，认为长期有效
        if (TextUtils.isEmpty(startTime) && TextUtils.isEmpty(endTime)) {
            return true;
        }

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date now = new Date();

            // 如果有开始时间，验证是否已开始
            if (!TextUtils.isEmpty(startTime)) {
                Date begin = sdf.parse(startTime);
                if (begin != null && now.before(begin)) {
                    return false;  // 还未开始
                }
            }

            // 如果有结束时间，验证是否已过期
            if (!TextUtils.isEmpty(endTime)) {
                Date end = sdf.parse(endTime);
                if (end != null && now.after(end)) {
                    return false;  // 已过期
                }
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Validate period failed", e);
            // 验证失败，默认为有效
            return true;
        }
    }

    private void setVerifying(boolean value) {
        verifying = value;
        if (btnVerify != null) {
            btnVerify.setEnabled(!value);
            btnVerify.setText(value ? "核验中..." : "开始核验");
        }
    }


    private void recordAndOpen(VerificationResult result) {
        recordAndOpen(result, currentCarDTO);  // 使用当前的 currentCarDTO
    }

    private void recordAndOpen(VerificationResult result, @Nullable CarCertificateDTO carDTO) {
        Intent intent = new Intent(requireContext(), CarAndPersonDetailActivity.class);
        intent.putExtra(CarAndPersonDetailActivity.EXTRA_RESULT, result);

        // 如果有 carDTO，总是传递给 VerificationDetailActivity（用于显示车证权限）
        if (carDTO != null) {
            intent.putExtra("car_dto", carDTO);
        }
        intent.putExtra(CommonConfig.EXTRA_IS_CAR, true);
        startActivity(intent);
    }

    private String getRequiredPermissionSummary(CarCertificateDTO carDTO) {
        if (carDTO == null) {
            return "无车辆信息";
        }

        try {
            BasicInfo basicInfo = initializationRepository.getBasicInfo();
            if (basicInfo == null) {
                Log.w(TAG, "getRequiredPermissionSummary: BasicInfo为null");
                return "未初始化";
            }

            String currentActiveId = AppPreferences.getLastActiveId(requireContext());
            DevicePermissionHelper.PermissionSets device =
                    DevicePermissionHelper.resolveDevicePermissions(
                            requireContext(), currentActiveId, basicInfo, ModuleType.VEHICLE);
            Log.d(TAG, "设备权限(matrixAuth) venue=" + device.venueCodes
                    + ", park(停车)=" + device.parkCodes);

            DevicePermissionHelper.MatchResult match =
                    DevicePermissionHelper.evaluateCarCertificateMatch(
                            carDTO.venueCodeChildren, carDTO.parkingCode, device);
            if (match.matched) {
                return "";
            }
            return !TextUtils.isEmpty(match.failReason) ? match.failReason : "缺少通行权限";
        } catch (Exception e) {
            Log.e(TAG, "获取所需权限失败", e);
            return "获取所需权限失败";
        }
    }

    @Override
    public void onTagDetected(String tagId, String[] techList) {
        Log.i(TAG, "onTagDetected(NFC) tagId=" + tagId
                + " → 车证页收到 NFC，仅更新 chipId，不会触发 UHF。若要手柄读车证，需把按键接到 startUhfInventory()");
        currentChipId = tagId;
        if (tvVehicleStatus != null) {
            tvVehicleStatus.setText("已读取芯片：" + tagId);
        }
    }

    @Override
    public void onDestroyView() {
        unregisterFunKeyReceiver();
        super.onDestroyView();
        if (initializationRepository != null) {
            initializationRepository.close();
            initializationRepository = null;
        }
        if (inventoryExecutor != null) {
            inventoryExecutor.shutdownNow();
            inventoryExecutor = null;
        }
        if (uhfManager != null) {
            try {
                uhfManager.close();
            } catch (Exception ignored) {
            }
            uhfManager = null;
        }
    }
}


