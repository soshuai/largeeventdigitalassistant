package com.largeevent.management;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.data.ActiveUserParser;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.EventInfo;
import com.largeevent.management.model.EventSession;

import com.largeevent.management.network.*;
import com.largeevent.management.network.dto.ActiveUserBaseDTO;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.EquipmentRegisterDTO;
import com.largeevent.management.widget.FlowLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class EventSettingsActivity extends AppCompatActivity {

    private static final String TAG = "EventSettingsActivity";

    private InitializationRepository repository;
    private TextView tvEventName;
    private FlowLayout containerSessions;
    private FlowLayout containerVenuePermissions;
    private FlowLayout containerAreaPermissions;
    private FlowLayout containerLocation;
    private Button btnSyncData;
    private TextView tvSyncStatus;
    private TextView tvPersonTotal;
    private TextView tvVehicleTotal;
    private Button btnViewDataDetail;
    private LinearLayout layoutSyncDetails;

    private final List<CheckBox> sessionCheckboxes = new ArrayList<>();
    private final List<CheckBox> venueCheckboxes = new ArrayList<>();
    private final List<CheckBox> areaCheckboxes = new ArrayList<>();
    private final List<CheckBox> locationCheckboxes = new ArrayList<>();

    private EventInfo currentEventInfo;
    private String currentActiveId;
    private String selectedLocationId = null; // 保存选中的位置ID

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_settings);

        repository = new InitializationRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // 设置 Toolbar 返回按钮点击事件
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvEventName = findViewById(R.id.tv_event_name);
        containerSessions = findViewById(R.id.container_sessions);
        containerVenuePermissions = findViewById(R.id.container_venue_permissions);
        containerAreaPermissions = findViewById(R.id.container_area_permissions);
        containerLocation = findViewById(R.id.container_location);
        btnSyncData = findViewById(R.id.btn_sync_data);
        tvSyncStatus = findViewById(R.id.tv_sync_status);
        tvPersonTotal = findViewById(R.id.tv_person_total);
        tvVehicleTotal = findViewById(R.id.tv_vehicle_total);
        btnViewDataDetail = findViewById(R.id.btn_view_data_detail);
        layoutSyncDetails = findViewById(R.id.layout_sync_details);

        btnSyncData.setOnClickListener(v -> syncData());
        btnViewDataDetail.setOnClickListener(v -> openDataDetailEntry());

        try {
            // 优先使用设置页传入的活动ID
            String fromIntentId = getIntent().getStringExtra("active_id");
            if (!TextUtils.isEmpty(fromIntentId)) {
                currentActiveId = fromIntentId;
                // 根据活动ID获取对应的基础信息
                fetchBasicInfoForActivity(fromIntentId);
            } else {
                refreshActiveId();
                currentEventInfo = repository.getEventInfo();
                renderEventInfo();
            }
            restoreSyncStatus();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            // 如果初始化失败，显示空状态
            if (currentEventInfo == null) {
                currentEventInfo = new EventInfo("未设置活动", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                renderEventInfo();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderEventInfo() {
        if (currentEventInfo == null) {
            currentEventInfo = new EventInfo("未设置活动", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        tvEventName.setText(currentEventInfo.name);
        populateSessionList();
        populatePermissionList();
    }

    private void refreshActiveId() {
        BasicInfo basicInfo = repository.getBasicInfo();
        if (basicInfo != null && !basicInfo.getActiveModels().isEmpty()) {
            currentActiveId = basicInfo.getActiveModels().get(0).id;
            AppPreferences.setLastActiveId(this, currentActiveId);
        } else {
            currentActiveId = AppPreferences.getLastActiveId(this);
        }
    }

    private void populateSessionList() {
        containerSessions.removeAllViews();
        sessionCheckboxes.clear();

        if (currentEventInfo == null || currentEventInfo.getSessions() == null) {
            return;
        }

        // 读取上次保存的场次选择（仅用于显示）
        Set<String> savedSessions = AppPreferences.getSelectedSessions(this, currentActiveId);

        for (EventSession session : currentEventInfo.getSessions()) {
            CheckBox checkBox = createCheckBox(session.getDisplayText());
            // 根据保存的数据设置选中状态
            checkBox.setChecked(savedSessions.contains(session.getDisplayText()));
            // 不自动保存，移除监听器
            containerSessions.addView(checkBox);
            sessionCheckboxes.add(checkBox);
        }
    }

    private void populatePermissionList() {
        containerVenuePermissions.removeAllViews();
        containerAreaPermissions.removeAllViews();
        containerLocation.removeAllViews();
        venueCheckboxes.clear();
        areaCheckboxes.clear();
        locationCheckboxes.clear();

        // 读取上次保存的权限选择（仅用于显示，按活动隔离）
        Set<String> savedVenues = AppPreferences.getSelectedVenuePermissions(this, currentActiveId);
        Set<String> savedAreas = AppPreferences.getSelectedAreaPermissions(this, currentActiveId);

        // 直接从 BasicInfo 中获取场馆权限（activeVenueModelList）
        BasicInfo basicInfo = repository.getBasicInfo();
        if (basicInfo != null) {
            // 场馆权限：从 activeVenueModelList 中获取
            if (basicInfo.getActiveVenues() != null) {
                for (BasicInfo.ActiveVenueModel venue : basicInfo.getActiveVenues()) {
                    if (venue != null && !TextUtils.isEmpty(venue.venueName)) {
                        CheckBox checkBox = createCheckBox(venue.venueName);
                        checkBox.setChecked(savedVenues.contains(venue.venueName));
                        containerVenuePermissions.addView(checkBox);
                        venueCheckboxes.add(checkBox);
                    }
                }
            }

            // 区域权限：展示人证区域和车证区域合并
            List<String> allCertPermissions = new ArrayList<>();

            // 添加人证区域权限（使用 positionModelList）
            if (basicInfo.getPositions() != null) {
                for (BasicInfo.PositionModel position : basicInfo.getPositions()) {
                    if (position != null && !TextUtils.isEmpty(position.name)) {
                        allCertPermissions.add(position.name);
                    }
                }
            }

            // 添加车证区域权限
            if (basicInfo.getCarCertTypes() != null) {
                for (BasicInfo.CertTypeModel certType : basicInfo.getCarCertTypes()) {
                    if (certType != null && !TextUtils.isEmpty(certType.dictValue)) {
                        allCertPermissions.add(certType.dictValue);
                    }
                }
            }

            // 显示合并后的区域权限
            for (String permission : allCertPermissions) {
                CheckBox checkBox = createCheckBox(permission);
                checkBox.setChecked(savedAreas.contains(permission));
                containerAreaPermissions.addView(checkBox);
                areaCheckboxes.add(checkBox);
            }

            // 显示位置选择(单选,内容与区域权限相同)
            populateLocationList(basicInfo, allCertPermissions);
        } else {
            // 如果没有 BasicInfo 数据，则使用 EventInfo 的逻辑（向后兼容）
            if (currentEventInfo != null) {
                // 场馆权限
                if (currentEventInfo.getVenuePermissions() != null) {
                    for (String permission : currentEventInfo.getVenuePermissions()) {
                        CheckBox checkBox = createCheckBox(permission);
                        checkBox.setChecked(savedVenues.contains(permission));
                        containerVenuePermissions.addView(checkBox);
                        venueCheckboxes.add(checkBox);
                    }
                }

                // 区域权限
                if (currentEventInfo.getAreaPermissions() != null) {
                    for (String permission : currentEventInfo.getAreaPermissions()) {
                        CheckBox checkBox = createCheckBox(permission);
                        checkBox.setChecked(savedAreas.contains(permission));
                        containerAreaPermissions.addView(checkBox);
                        areaCheckboxes.add(checkBox);
                    }
                }
            }
        }
    }

    /**
     * 填充位置选择列表(单选,内容与区域权限相同)
     */
    private void populateLocationList(BasicInfo basicInfo, List<String> allCertPermissions) {
        if (basicInfo == null || allCertPermissions == null || allCertPermissions.isEmpty()) {
            return;
        }

        // 读取上次保存的位置ID
        String savedLocationId = AppPreferences.getSelectedLocationId(this, currentActiveId);
        selectedLocationId = savedLocationId;

        // 创建一个Map来存储 permission -> id 的映射
        final java.util.Map<String, String> permissionToIdMap = new java.util.HashMap<>();

        // 优先使用新的 locationInfoList
        if (basicInfo.getLocationInfoList() != null && !basicInfo.getLocationInfoList().isEmpty()) {
            for (BasicInfo.LocationInfo locationInfo : basicInfo.getLocationInfoList()) {
                if (locationInfo != null && !TextUtils.isEmpty(locationInfo.locationName) && !TextUtils.isEmpty(locationInfo.locationId)) {
                    permissionToIdMap.put(locationInfo.locationName, locationInfo.locationId);
                }
            }
        }

        // 如果 locationInfoList 为空，回退到旧的 positions
        if (permissionToIdMap.isEmpty() && basicInfo.getPositions() != null) {
            for (BasicInfo.PositionModel position : basicInfo.getPositions()) {
                if (position != null && !TextUtils.isEmpty(position.name) && !TextUtils.isEmpty(position.positionCode)) {
                    permissionToIdMap.put(position.name, position.positionCode);
                }
            }
        }

        // 从车证区域获取 dictCode 作为 ID
        if (basicInfo.getCarCertTypes() != null) {
            for (BasicInfo.CertTypeModel certType : basicInfo.getCarCertTypes()) {
                if (certType != null && !TextUtils.isEmpty(certType.dictValue) && !TextUtils.isEmpty(certType.dictCode)) {
                    permissionToIdMap.put(certType.dictValue, certType.dictCode);
                }
            }
        }

        // 显示位置选择(单选)
        for (String permission : allCertPermissions) {
            CheckBox checkBox = createCheckBox(permission);
            String itemId = permissionToIdMap.get(permission);

            // 设置选中状态
            if (itemId != null && itemId.equals(savedLocationId)) {
                checkBox.setChecked(true);
            }

            // 单选逻辑:点击时取消其他选项
            checkBox.setOnClickListener(v -> {
                // 取消其他所有选项
                for (CheckBox cb : locationCheckboxes) {
                    if (cb != checkBox) {
                        cb.setChecked(false);
                    }
                }

                // 更新选中的位置ID
                if (checkBox.isChecked()) {
                    selectedLocationId = itemId;
                    // 位置选择后调用设备注册接口
                    registerEquipment(itemId);
                } else {
                    selectedLocationId = null;
                }
            });

            containerLocation.addView(checkBox);
            locationCheckboxes.add(checkBox);
        }
    }

    private CheckBox createCheckBox(String text) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setTextColor(0xFF4A4F63);
        checkBox.setTextSize(14f);
        // 使用 MarginLayoutParams 适配 FlowLayout
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dpToPx(8);
        params.bottomMargin = dpToPx(8);
        checkBox.setLayoutParams(params);
        return checkBox;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 获取当前选中的场次
     */
    private Set<String> getCurrentSelectedSessions() {
        Set<String> selectedSessions = new java.util.HashSet<>();
        for (CheckBox cb : sessionCheckboxes) {
            if (cb.isChecked()) {
                selectedSessions.add(cb.getText().toString());
            }
        }
        return selectedSessions;
    }

    /**
     * 获取当前选中的权限
     */
    private Set<String> getCurrentSelectedVenuePermissions() {
        Set<String> selectedVenues = new java.util.HashSet<>();
        for (CheckBox cb : venueCheckboxes) {
            if (cb.isChecked()) {
                selectedVenues.add(cb.getText().toString());
            }
        }
        return selectedVenues;
    }

    private Set<String> getCurrentSelectedAreaPermissions() {
        Set<String> selectedAreas = new java.util.HashSet<>();
        for (CheckBox cb : areaCheckboxes) {
            if (cb.isChecked()) {
                selectedAreas.add(cb.getText().toString());
            }
        }
        return selectedAreas;
    }

    /**
     * 将当前选中状态返回给 SettingsFragment
     */
    @Override
    public void onBackPressed() {
        // 保存选中的位置ID
        if (!TextUtils.isEmpty(selectedLocationId)) {
            AppPreferences.setSelectedLocationId(this, currentActiveId, selectedLocationId);
        }

        Intent result = new Intent();
        result.putStringArrayListExtra("selected_sessions", new ArrayList<>(getCurrentSelectedSessions()));
        result.putStringArrayListExtra("selected_venues", new ArrayList<>(getCurrentSelectedVenuePermissions()));
        result.putStringArrayListExtra("selected_areas", new ArrayList<>(getCurrentSelectedAreaPermissions()));
        setResult(RESULT_OK, result);
        super.onBackPressed();
    }


    private void syncData() {
        String baseUrl = AppPreferences.getServerUrl(this);
        if (TextUtils.isEmpty(baseUrl)) {
            Toast.makeText(this, "请先设置服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(currentActiveId)) {
            Toast.makeText(this, "当前活动信息缺失，请先完成初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSyncData.setEnabled(false);
        btnSyncData.setText("同步中...");

        ApiService apiService = NetworkManager.getInstance().getApiService();
        retrofit2.Call<ApiResponse<java.util.List<ActiveUserBaseDTO>>> call =
                apiService.getActiveUser(currentActiveId, null);

        call.enqueue(new retrofit2.Callback<ApiResponse<List<ActiveUserBaseDTO>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveUserBaseDTO>>> call,
                                   @NonNull retrofit2.Response<ApiResponse<java.util.List<ActiveUserBaseDTO>>> response) {
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        handleSyncFailure("数据同步失败：服务器错误");
                        return;
                    }

                    ApiResponse<java.util.List<ActiveUserBaseDTO>> apiResponse = response.body();
                    if (apiResponse == null || apiResponse.getCode() != 200) {
                        handleSyncFailure("数据同步失败：" + (apiResponse != null ? apiResponse.getMessage() : "接口返回异常"));
                        return;
                    }

                    try {
                        java.util.List<ActiveUserBaseDTO> dataList = apiResponse.getData();
                        JSONArray dataArray = new JSONArray();
                        if (dataList != null) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            for (ActiveUserBaseDTO dto : dataList) {
                                String json = gson.toJson(dto);
                                dataArray.put(new JSONObject(json));
                            }
                        }
                        repository.saveActiveUsers(baseUrl, dataArray.toString());
                        ActiveUserParser.ActiveUserStats stats = ActiveUserParser.calculateStats(dataArray);
                        handleSyncSuccess(stats.personCount, stats.vehicleCount, "数据同步成功");
                    } catch (Exception e) {
                        handleSyncFailure("数据同步失败：" + e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveUserBaseDTO>>> call,
                                  @NonNull Throwable t) {
                runOnUiThread(() -> {
                    handleSyncFailure("数据同步失败：" + t.getMessage());
                });
            }
        });
    }

    /**
     * 设备注册接口
     * 选完位置后调用，用于注册设备并获取通行权限
     */
    private void registerEquipment(String locationId) {
        if (TextUtils.isEmpty(currentActiveId)) {
            Log.w(TAG, "registerEquipment: currentActiveId is null, skip registration");
            return;
        }

        String deviceCode = AppPreferences.getDeviceCode(this);
        if (TextUtils.isEmpty(deviceCode)) {
            deviceCode = generateDeviceCode();
            AppPreferences.setDeviceCode(this, deviceCode);
        }

        String createTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

        EquipmentRegisterDTO eqpRegisterDTO = new EquipmentRegisterDTO();
        eqpRegisterDTO.activityId = currentActiveId;
        eqpRegisterDTO.eqpID = deviceCode;
        eqpRegisterDTO.createTime = createTime;
        eqpRegisterDTO.locationID = locationId;
        eqpRegisterDTO.zoneLocationID = locationId;

        ApiService apiService = NetworkManager.getInstance().getApiService();
        retrofit2.Call<ResponseBody> call = apiService.registerEquipment(eqpRegisterDTO);

        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "registerEquipment: success");
                    AppPreferences.setSelectedLocationId(EventSettingsActivity.this, currentActiveId, locationId);
                } else {
                    Log.w(TAG, "registerEquipment: failed, code=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "registerEquipment: onFailure", t);
            }
        });
    }

    private String generateDeviceCode() {
        return "DEV_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    private void handleSyncSuccess(int personCount, int vehicleCount, String message) {
        // 隐藏同步按钮
        btnSyncData.setVisibility(android.view.View.GONE);

        // 显示同步状态
        tvSyncStatus.setText(message);
        tvSyncStatus.setTextColor(0xFF52C41A);
        tvSyncStatus.setVisibility(android.view.View.VISIBLE);

        // 显示人证车证数据
        tvPersonTotal.setText("人证：" + personCount + " 条");
        tvVehicleTotal.setText("车证：" + vehicleCount + " 条");
        layoutSyncDetails.setVisibility(android.view.View.VISIBLE);

        // 显示查看数据详情按钮
        btnViewDataDetail.setVisibility(android.view.View.VISIBLE);

        // 保存同步结果
        AppPreferences.setLastSyncResult(this, personCount, vehicleCount, message);
    }

    private void handleSyncFailure(String errorMessage) {
        // 恢复同步按钮
        btnSyncData.setEnabled(true);
        btnSyncData.setText("同步数据");
        //        btnSyncData.setVisibility(android.view.View.VISIBLE);

        // 只显示错误信息
        tvSyncStatus.setText(errorMessage);
        tvSyncStatus.setTextColor(0xFFFF4D4F);
        tvSyncStatus.setVisibility(android.view.View.VISIBLE);

        // 隐藏其他视图
        layoutSyncDetails.setVisibility(android.view.View.GONE);
        btnViewDataDetail.setVisibility(android.view.View.GONE);

        // 显示提示信息
        Toast.makeText(this, "请检查网络配置", Toast.LENGTH_SHORT).show();
    }

    private void restoreSyncStatus() {
        int person = AppPreferences.getLastPersonCount(this);
        int vehicle = AppPreferences.getLastVehicleCount(this);
        String message = AppPreferences.getLastSyncStatus(this);

        if (TextUtils.isEmpty(message)) {
            // 未同步过，显示同步按钮，隐藏其他
            //            btnSyncData.setVisibility(android.view.View.VISIBLE);
            tvSyncStatus.setVisibility(android.view.View.GONE);
            layoutSyncDetails.setVisibility(android.view.View.GONE);
            btnViewDataDetail.setVisibility(android.view.View.GONE);
        } else {
            // 之前同步过，显示同步结果
            if (person > 0 || vehicle > 0) {
                // 同步成功，隐藏同步按钮，显示所有信息
                btnSyncData.setVisibility(android.view.View.GONE);
                tvSyncStatus.setText(message);
                tvSyncStatus.setTextColor(0xFF52C41A);
                tvSyncStatus.setVisibility(android.view.View.VISIBLE);
                tvPersonTotal.setText("人证：" + person + " 条");
                tvVehicleTotal.setText("车证：" + vehicle + " 条");
                layoutSyncDetails.setVisibility(android.view.View.VISIBLE);
                btnViewDataDetail.setVisibility(android.view.View.VISIBLE);
            } else {
                // 同步失败，显示同步按钮和错误信息
                //                btnSyncData.setVisibility(android.view.View.VISIBLE);
                tvSyncStatus.setVisibility(android.view.View.GONE);
                layoutSyncDetails.setVisibility(android.view.View.GONE);
                btnViewDataDetail.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void openDataDetailEntry() {
        startActivity(new Intent(this, DataDetailEntryActivity.class));
    }

    private String ensureEndsWithSlash(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return raw;
        }
        if (raw.endsWith("/")) {
            return raw;
        }
        return raw + "/";
    }

    /**
     * 根据活动ID获取对应的基础信息
     */
    private void fetchBasicInfoForActivity(String activeId) {
        // 首先从活动列表中查找对应的活动
        List<BasicInfo.ActiveModel> activeList = repository.getActiveList();
        BasicInfo.ActiveModel targetActive = null;

        if (activeList != null) {
            for (BasicInfo.ActiveModel model : activeList) {
                if (model != null && activeId.equals(model.id)) {
                    targetActive = model;
                    break;
                }
            }
        }

        // 如果活动列表为空，尝试从 BasicInfo 中查找
        if (targetActive == null) {
            BasicInfo basicInfo = repository.getBasicInfo();
            if (basicInfo != null && basicInfo.getActiveModels() != null) {
                for (BasicInfo.ActiveModel model : basicInfo.getActiveModels()) {
                    if (model != null && activeId.equals(model.id)) {
                        targetActive = model;
                        break;
                    }
                }
            }
        }

        if (targetActive == null) {
            // 如果找不到活动，使用默认逻辑
            currentEventInfo = repository.getEventInfo();
            renderEventInfo();
            return;
        }

        String eventCode = targetActive.id;  // 使用活动ID作为eventCode
        if (TextUtils.isEmpty(eventCode)) {
            // 如果 eventCode 为空，使用默认逻辑
            currentEventInfo = repository.getEventInfo();
            renderEventInfo();
            return;
        }

        // 设置活动名称
        final String activeName = targetActive.activeName != null ? targetActive.activeName : "未设置活动";

        // 获取服务器地址
        String baseUrl = AppPreferences.getServerUrl(this);
        if (TextUtils.isEmpty(baseUrl)) {
            // 如果没有服务器地址，使用默认逻辑
            currentEventInfo = repository.getEventInfo();
            renderEventInfo();
            return;
        }

        // 调用接口获取该活动的基础信息
        ApiService apiService = NetworkManager.getInstance().getApiService();
        retrofit2.Call<ResponseBody> call = apiService.getBasicInfo(eventCode);

        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call,
                                   @NonNull retrofit2.Response<ResponseBody> response) {
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        Toast.makeText(EventSettingsActivity.this,
                                "基础信息获取失败：服务器错误", Toast.LENGTH_SHORT).show();
                        currentEventInfo = repository.getEventInfo();
                        renderEventInfo();
                        return;
                    }

                    try {
                        String respText = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(respText);
                        if (json.optInt("code", -1) != 200) {
                            Toast.makeText(EventSettingsActivity.this,
                                    "基础信息获取失败：" + json.optString("message", "接口返回异常"),
                                    Toast.LENGTH_SHORT).show();
                            currentEventInfo = repository.getEventInfo();
                            renderEventInfo();
                            return;
                        }

                        // 保存基础信息
                        try {
                            repository.saveBaseInfo(baseUrl, respText);
                            // 重新获取 EventInfo
                            currentEventInfo = repository.getEventInfo();
                            // 如果 EventInfo 为空或名称不对，手动设置活动名称
                            if (currentEventInfo == null) {
                                currentEventInfo = new EventInfo(activeName, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                            }
                            renderEventInfo();
                        } catch (Exception ex) {
                            Toast.makeText(EventSettingsActivity.this,
                                    "基础信息保存失败：" + ex.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            currentEventInfo = repository.getEventInfo();
                            renderEventInfo();
                        }
                    } catch (Exception e) {
                        Toast.makeText(EventSettingsActivity.this,
                                "基础信息处理失败：" + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        currentEventInfo = repository.getEventInfo();
                        renderEventInfo();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(EventSettingsActivity.this,
                            "基础信息获取失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    // 失败时使用默认逻辑
                    currentEventInfo = repository.getEventInfo();
                    renderEventInfo();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.close();
        }
    }
}