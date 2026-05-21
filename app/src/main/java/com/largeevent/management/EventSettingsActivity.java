package com.largeevent.management;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.data.ActiveUserParser;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.data.DevicePermissionHelper;
import com.largeevent.management.data.MatrixAuthSelectionHelper;
import com.largeevent.management.data.PersonVerificationHelper;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.BasicInfo.LocationInfo;
import com.largeevent.management.model.BasicInfo.VenueInfo;
import com.largeevent.management.model.EventInfo;
import com.largeevent.management.model.EventSession;

import com.largeevent.management.network.*;
import com.largeevent.management.network.dto.ActiveUserBaseDTO;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.EquipmentRegisterDTO;
import com.largeevent.management.network.dto.MatrixAuthInfoDTO;
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
    private FlowLayout containerPartitionPermissions;
    private FlowLayout containerCertZonePermissions;
    private Spinner spinnerSubUnit;
    private Spinner spinnerEqpType;
    private SwitchCompat switchActivationCheck;
    private Spinner spinnerLocation;
    private Spinner spinnerZone;
    private String selectedSubUnitName;
    private Button btnSyncData;
    private TextView tvSyncStatus;
    private TextView tvPersonTotal;
    private TextView tvVehicleTotal;
    private Button btnViewDataDetail;
    private LinearLayout layoutSyncDetails;

    private final List<CheckBox> sessionCheckboxes = new ArrayList<>();
    private final List<CheckBox> venueCheckboxes = new ArrayList<>();
    private final List<CheckBox> partitionCheckboxes = new ArrayList<>();
    private final List<CheckBox> certZoneCheckboxes = new ArrayList<>();

    private EventInfo currentEventInfo;
    private String currentActiveId;
    private String selectedLocationId = null; // 保存选中的位置ID
    private String selectedZoneId = null; // 保存选中的分区ID
    
    // 用于存储位置和分区数据
    private List<LocationInfo> locationList = new ArrayList<>(); // locationType="cg" 的数据
    private List<LocationInfo> zoneList = new ArrayList<>(); // locationType="fq" 的数据

    private boolean suppressSpinnerCallback = false;
    private boolean isRegistering = false;

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
        containerPartitionPermissions = findViewById(R.id.container_partition_permissions);
        containerCertZonePermissions = findViewById(R.id.container_cert_zone_permissions);
        spinnerSubUnit = findViewById(R.id.spinner_sub_unit);
        spinnerEqpType = findViewById(R.id.spinner_eqp_type);
        switchActivationCheck = findViewById(R.id.switch_activation_check);
        spinnerLocation = findViewById(R.id.spinner_location);
        spinnerZone = findViewById(R.id.spinner_zone);
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
            CheckBox checkBox = createCheckBox(session.getDisplayText(), null);
            // 根据保存的数据设置选中状态
            checkBox.setChecked(savedSessions.contains(session.getDisplayText()));
            // 不自动保存，移除监听器
            containerSessions.addView(checkBox);
            sessionCheckboxes.add(checkBox);
        }
    }

    private void populatePermissionList() {
        containerVenuePermissions.removeAllViews();
        containerPartitionPermissions.removeAllViews();
        containerCertZonePermissions.removeAllViews();
        venueCheckboxes.clear();
        partitionCheckboxes.clear();
        certZoneCheckboxes.clear();

        // 读取上次保存的权限选择（仅用于显示，按活动隔离）
        Set<String> savedVenues = AppPreferences.getSelectedVenuePermissions(this, currentActiveId);
        Set<String> savedPartitions = AppPreferences.getSelectedAreaPermissions(this, currentActiveId);
        Set<String> savedCertZones = AppPreferences.getSelectedCertZonePermissions(this, currentActiveId);

        // 直接从 BasicInfo 中获取数据
        BasicInfo basicInfo = repository.getBasicInfo();
        if (basicInfo != null) {
            // 清空之前的数据
            locationList.clear();
            zoneList.clear();
            
            // 从 locationInfoList 获取位置和分区数据
            if (basicInfo.getLocationInfoList() != null) {
                for (BasicInfo.LocationInfo locationInfo : basicInfo.getLocationInfoList()) {
                    if (locationInfo != null && !TextUtils.isEmpty(locationInfo.locationName) && !TextUtils.isEmpty(locationInfo.locationId)) {
                        String locationType = locationInfo.locationType;
                        if ("cg".equals(locationType)) {
                            // 场馆类型 - 用于位置选择
                            locationList.add(locationInfo);
                        } else if ("fq".equals(locationType)) {
                            // 区域类型 - 用于分区选择
                            zoneList.add(locationInfo);
                        }
                    }
                }
            }

            // 设置位置/分区下拉（抑制初始化回调，避免重复拉取设备权限）
            suppressSpinnerCallback = true;
            setupLocationSpinner();
            setupZoneSpinner();
            suppressSpinnerCallback = false;

            // 场馆权限（venueInfoList）、分区权限（personCertAreaList）、区域权限（personCertZoneList）
            populateVenuePermissions(basicInfo, savedVenues);
            populateDictPermissions(basicInfo.getPersonCertAreaList(), savedPartitions,
                    containerPartitionPermissions, partitionCheckboxes);
            populateDictPermissions(basicInfo.getPersonCertZoneList(), savedCertZones,
                    containerCertZonePermissions, certZoneCheckboxes);

            setupEqpTypeSpinner();
            // 子活动、证件激活校验入口暂隐藏，逻辑保留默认：不筛选子活动、不启用激活校验
            if (switchActivationCheck != null) {
                switchActivationCheck.setChecked(false);
            }
        } else {
            // 如果没有 BasicInfo 数据，则使用 EventInfo 的逻辑（向后兼容）
            if (currentEventInfo != null) {
                // 场馆权限
                if (currentEventInfo.getVenuePermissions() != null) {
                    for (String permission : currentEventInfo.getVenuePermissions()) {
                        CheckBox checkBox = createCheckBox(permission, null);
                        checkBox.setChecked(savedVenues.contains(permission));
                        containerVenuePermissions.addView(checkBox);
                        venueCheckboxes.add(checkBox);
                    }
                }
            }
        }
    }

    private void setupSubUnitSpinner(BasicInfo basicInfo) {
        List<String> names = new ArrayList<>();
        names.add("全部子活动");
        selectedSubUnitName = AppPreferences.getSelectedSubUnitName(this, currentActiveId);
        int selectedIndex = 0;
        if (basicInfo != null && !basicInfo.getActiveModels().isEmpty()) {
            BasicInfo.ActiveModel model = basicInfo.getActiveModels().get(0);
            if (model != null && model.getActiveUnitModelList() != null) {
                int idx = 1;
                for (BasicInfo.ActiveUnitModel unit : model.getActiveUnitModelList()) {
                    if (unit != null && !TextUtils.isEmpty(unit.getUnitName())) {
                        names.add(unit.getUnitName());
                        if (unit.getUnitName().equals(selectedSubUnitName)) {
                            selectedIndex = idx;
                        }
                        idx++;
                    }
                }
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubUnit.setAdapter(adapter);
        spinnerSubUnit.setSelection(selectedIndex);
        spinnerSubUnit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                if (position == 0) {
                    selectedSubUnitName = null;
                } else {
                    selectedSubUnitName = names.get(position);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedSubUnitName = null;
            }
        });
    }

    private void setupEqpTypeSpinner() {
        final String[] types = {
                PersonVerificationHelper.EQP_HANDHELD,
                PersonVerificationHelper.EQP_CHANNEL,
                PersonVerificationHelper.EQP_GATE
        };
        String saved = AppPreferences.getEqpType(this, currentActiveId);
        int selectedIndex = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(saved)) {
                selectedIndex = i;
                break;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEqpType.setAdapter(adapter);
        spinnerEqpType.setSelection(selectedIndex);
        spinnerEqpType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                AppPreferences.setEqpType(EventSettingsActivity.this, currentActiveId, types[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    /**
     * 设置位置下拉选择（场馆类型 locationType="cg"）
     */
    private void setupLocationSpinner() {
        // 读取上次保存的位置ID
        String savedLocationId = AppPreferences.getSelectedLocationId(this, currentActiveId);
        selectedLocationId = savedLocationId;

        List<String> locationNames = new ArrayList<>();
        locationNames.add("请选择设备所在位置");
        
        int selectedIndex = 0;
        for (int i = 0; i < locationList.size(); i++) {
            LocationInfo location = locationList.get(i);
            locationNames.add(location.locationName);
            if (location.locationId.equals(savedLocationId)) {
                selectedIndex = i + 1; // +1 是因为第一个是提示项
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(adapter);
        spinnerLocation.setSelection(selectedIndex);

        android.widget.AdapterView.OnItemSelectedListener locationListener =
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                               int position, long id) {
                        if (suppressSpinnerCallback) {
                            return;
                        }
                        if (position == 0) {
                            selectedLocationId = null;
                        } else {
                            LocationInfo location = locationList.get(position - 1);
                            selectedLocationId = location.locationId;
                        }
                        onLocationOrZoneChanged();
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        if (suppressSpinnerCallback) {
                            return;
                        }
                        selectedLocationId = null;
                    }
                };
        spinnerLocation.setOnItemSelectedListener(locationListener);
    }

    /**
     * 设置分区下拉选择（区域类型 locationType="fq"）
     */
    private void setupZoneSpinner() {
        // 读取上次保存的分区ID
        String savedZoneId = AppPreferences.getSelectedZoneId(this, currentActiveId);
        selectedZoneId = savedZoneId;

        List<String> zoneNames = new ArrayList<>();
        zoneNames.add("请选择设备所在分区");
        
        int selectedIndex = 0;
        for (int i = 0; i < zoneList.size(); i++) {
            LocationInfo zone = zoneList.get(i);
            zoneNames.add(zone.locationName);
            if (zone.locationId.equals(savedZoneId)) {
                selectedIndex = i + 1; // +1 是因为第一个是提示项
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, zoneNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZone.setAdapter(adapter);
        spinnerZone.setSelection(selectedIndex);

        android.widget.AdapterView.OnItemSelectedListener zoneListener =
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                               int position, long id) {
                        if (suppressSpinnerCallback) {
                            return;
                        }
                        if (position == 0) {
                            selectedZoneId = null;
                        } else {
                            LocationInfo zone = zoneList.get(position - 1);
                            selectedZoneId = zone.locationId;
                        }
                        onLocationOrZoneChanged();
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        if (suppressSpinnerCallback) {
                            return;
                        }
                        selectedZoneId = null;
                    }
                };
        spinnerZone.setOnItemSelectedListener(zoneListener);
    }

    /**
     * 位置与分区均选择后注册设备并拉取权限。
     * 若该活动已在「保存设置」后同步过相同位置+分区，则不再请求（换活动或改位置/分区后会重新拉取）。
     */
    private void onLocationOrZoneChanged() {
        if (TextUtils.isEmpty(selectedLocationId) || TextUtils.isEmpty(selectedZoneId)) {
            return;
        }
        if (TextUtils.isEmpty(currentActiveId) || isRegistering) {
            return;
        }
        if (AppPreferences.isDeviceAuthSynced(
                this, currentActiveId, selectedLocationId, selectedZoneId)) {
            Log.d(TAG, "设备权限已同步，跳过注册与 getMatrixAuthInfoList");
            return;
        }
        registerEquipmentAndLoadMatrixAuth();
    }

    /**
     * 填充场馆权限（从 venueInfoList 获取）
     */
    private void populateVenuePermissions(BasicInfo basicInfo, Set<String> savedVenues) {
        populateDictPermissions(basicInfo.getVenueInfoList(), savedVenues,
                containerVenuePermissions, venueCheckboxes);
    }

    /**
     * 填充字典类权限选项（场馆/分区/区域）
     */
    private void populateDictPermissions(List<VenueInfo> dictList, Set<String> savedSelections,
                                         FlowLayout container, List<CheckBox> checkboxList) {
        if (dictList == null || dictList.isEmpty()) {
            return;
        }
        for (VenueInfo item : dictList) {
            if (item != null && !TextUtils.isEmpty(item.dictValue)) {
                CheckBox checkBox = createCheckBox(item.dictValue, item.dictCode);
                checkBox.setChecked(savedSelections.contains(item.dictValue));
                container.addView(checkBox);
                checkboxList.add(checkBox);
            }
        }
    }

    private void registerEquipmentAndLoadMatrixAuth() {
        String eqpId = ensureDeviceCode();
        if (TextUtils.isEmpty(eqpId)) {
            Toast.makeText(this, "设备编号为空，无法注册", Toast.LENGTH_SHORT).show();
            return;
        }

        isRegistering = true;
        Toast.makeText(this, "正在注册设备并获取权限...", Toast.LENGTH_SHORT).show();

        ApiService apiService = NetworkManager.getInstance().getApiService();
        EquipmentRegisterDTO dto = buildEquipmentRegisterDTO(eqpId);

        apiService.registerEquipment(dto).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call,
                                   @NonNull retrofit2.Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    isRegistering = false;
                    Toast.makeText(EventSettingsActivity.this,
                            "设备注册失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                fetchMatrixAuthInfo(eqpId);
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                isRegistering = false;
                Toast.makeText(EventSettingsActivity.this,
                        "设备注册失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMatrixAuthInfo(String eqpId) {
        ApiService apiService = NetworkManager.getInstance().getApiService();
        apiService.getMatrixAuthInfoList(currentActiveId, eqpId)
                .enqueue(new retrofit2.Callback<ApiResponse<java.util.List<MatrixAuthInfoDTO>>>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<ApiResponse<java.util.List<MatrixAuthInfoDTO>>> call,
                                           @NonNull retrofit2.Response<ApiResponse<java.util.List<MatrixAuthInfoDTO>>> response) {
                        isRegistering = false;
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(EventSettingsActivity.this,
                                    "获取设备权限失败", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ApiResponse<java.util.List<MatrixAuthInfoDTO>> apiResponse = response.body();
                        if (apiResponse.getCode() != 200) {
                            Toast.makeText(EventSettingsActivity.this,
                                    "获取设备权限失败：" + apiResponse.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        java.util.List<MatrixAuthInfoDTO> authList = apiResponse.getData();
                        DevicePermissionHelper.PermissionSets sets =
                                DevicePermissionHelper.fromMatrixAuthDtoList(authList);
                        AppPreferences.setDeviceMatrixAuthCodes(
                                EventSettingsActivity.this, currentActiveId, sets);
                        AppPreferences.setDevicePermissionConfigured(
                                EventSettingsActivity.this, currentActiveId, sets.hasAnyConfigured());
                        MatrixAuthSelectionHelper.applyMatrixAuthToCheckboxes(
                                authList, venueCheckboxes, partitionCheckboxes, certZoneCheckboxes);
                        appendMissingVenueCheckboxes(authList);
                        Toast.makeText(EventSettingsActivity.this,
                                "已加载后台设备权限，可按需调整", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull retrofit2.Call<ApiResponse<java.util.List<MatrixAuthInfoDTO>>> call,
                                          @NonNull Throwable t) {
                        isRegistering = false;
                        Toast.makeText(EventSettingsActivity.this,
                                "获取设备权限失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 设备权限里的场馆 code 若不在 getBasicInfo.venueInfoList，补充勾选项（如 SGG、INF）。
     */
    private void appendMissingVenueCheckboxes(java.util.List<MatrixAuthInfoDTO> authList) {
        if (authList == null) {
            return;
        }
        java.util.Set<String> dictCodes = new java.util.HashSet<>();
        BasicInfo basicInfo = repository.getBasicInfo();
        if (basicInfo != null && basicInfo.getVenueInfoList() != null) {
            for (BasicInfo.VenueInfo v : basicInfo.getVenueInfoList()) {
                if (v != null && !TextUtils.isEmpty(v.dictCode)) {
                    dictCodes.add(v.dictCode.trim());
                }
            }
        }
        java.util.Map<String, String> missing =
                MatrixAuthSelectionHelper.collectVenueCodesNotInDict(authList, dictCodes);
        for (java.util.Map.Entry<String, String> entry : missing.entrySet()) {
            CheckBox checkBox = createCheckBox(entry.getValue(), entry.getKey());
            checkBox.setChecked(true);
            containerVenuePermissions.addView(checkBox);
            venueCheckboxes.add(checkBox);
        }
    }

    private EquipmentRegisterDTO buildEquipmentRegisterDTO(String eqpId) {
        EquipmentRegisterDTO dto = new EquipmentRegisterDTO();
        dto.accountNumber = "";
        dto.activityId = currentActiveId;
        dto.carNumber = "";
        dto.createTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());
        dto.dataState = 1;
        dto.description = "";
        dto.eqpCode = eqpId;
        dto.eqpID = eqpId;
        dto.eqpIP = getDeviceIpAddress();
        dto.eqpModel = android.os.Build.MODEL;
        dto.eqpName = "手持设备1";
        dto.eqpState = 0;
        dto.eqpType = AppPreferences.getEqpType(this, currentActiveId);
        dto.locationID = selectedLocationId;
        dto.zoneLocationID = selectedZoneId;
        return dto;
    }

    private String ensureDeviceCode() {
        String code = AppPreferences.getDeviceCode(this);
        if (TextUtils.isEmpty(code)) {
            code = "DEV_" + System.currentTimeMillis();
            AppPreferences.setDeviceCode(this, code);
        }
        return code;
    }

    private String getDeviceIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (java.net.SocketException e) {
            Log.w(TAG, "getDeviceIpAddress failed", e);
        }
        return "192.168.1.100";
    }

    private CheckBox createCheckBox(String text, String dictCode) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        if (!TextUtils.isEmpty(dictCode)) {
            checkBox.setTag(dictCode);
        }
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
        return getCheckedTexts(venueCheckboxes);
    }

    private Set<String> getCurrentSelectedPartitionPermissions() {
        return getCheckedTexts(partitionCheckboxes);
    }

    private Set<String> getCurrentSelectedCertZonePermissions() {
        return getCheckedTexts(certZoneCheckboxes);
    }

    private Set<String> getCheckedTexts(List<CheckBox> checkboxes) {
        Set<String> selected = new java.util.HashSet<>();
        for (CheckBox cb : checkboxes) {
            if (cb.isChecked()) {
                selected.add(cb.getText().toString());
            }
        }
        return selected;
    }

    /**
     * 将当前选中状态返回给 SettingsFragment
     */
    @Override
    public void onBackPressed() {
        // 返回选中数据给 SettingsFragment，仅在设置页点击「保存设置」时持久化
        returnResultAndFinish();
    }

    /**
     * 返回选中的数据给 SettingsFragment
     */
    private void returnResultAndFinish() {
        Intent result = new Intent();
        result.putStringArrayListExtra("selected_sessions", new ArrayList<>(getCurrentSelectedSessions()));
        result.putStringArrayListExtra("selected_venues", new ArrayList<>(getCurrentSelectedVenuePermissions()));
        result.putStringArrayListExtra("selected_areas", new ArrayList<>(getCurrentSelectedPartitionPermissions()));
        result.putStringArrayListExtra("selected_cert_zones", new ArrayList<>(getCurrentSelectedCertZonePermissions()));
        result.putExtra("selected_location_id", selectedLocationId);
        result.putExtra("selected_zone_id", selectedZoneId);
        result.putExtra("activation_check_enabled", false);
        result.putExtra("selected_sub_unit", (String) null);
        if (spinnerEqpType != null && spinnerEqpType.getSelectedItem() != null) {
            result.putExtra("eqp_type", spinnerEqpType.getSelectedItem().toString());
        }
        boolean permConfigured = !getCurrentSelectedVenuePermissions().isEmpty()
                || !getCurrentSelectedPartitionPermissions().isEmpty()
                || !getCurrentSelectedCertZonePermissions().isEmpty();
        result.putExtra("device_perm_configured", permConfigured);
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