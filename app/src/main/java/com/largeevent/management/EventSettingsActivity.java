package com.largeevent.management;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.largeevent.management.data.ModuleType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.BasicInfo.LocationInfo;
import com.largeevent.management.model.EventInfo;
import com.largeevent.management.model.EventSession;

import com.largeevent.management.network.*;
import com.largeevent.management.network.dto.ActiveUserBaseVo;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.EquipmentRegisterAo;
import com.largeevent.management.network.dto.MatrixAuthInfoVo;
import com.largeevent.management.widget.FlowLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private TextView tvDeviceId;
    private FlowLayout containerSessions;
    private FlowLayout containerVenuePermissions;
    private FlowLayout containerPartitionPermissions;
    private FlowLayout containerCertZonePermissions;
    private FlowLayout containerParkingPermissions;
    private TextView tvLabelPartitionPermission;
    private TextView tvLabelAreaPermission;
    private TextView tvLabelParkingPermission;
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
    private final List<TextView> venuePermissionChips = new ArrayList<>();
    private final List<TextView> partitionPermissionChips = new ArrayList<>();
    private final List<TextView> certZonePermissionChips = new ArrayList<>();
    private final List<TextView> parkingPermissionChips = new ArrayList<>();

    private EventInfo currentEventInfo;
    private String currentActiveId;
    private String selectedLocationId = null; // 保存选中的位置ID
    private String selectedZoneId = null; // 保存选中的分区ID
    /** 当前编辑的业务模块：1 人证 / 2 车证 */
    private int currentModuleType = ModuleType.PERSON;
    
    // 用于存储位置和分区数据（已按当前模块过滤）
    private List<LocationInfo> locationList = new ArrayList<>(); // locationType="cg"
    private List<LocationInfo> zoneList = new ArrayList<>(); // locationType="fq"
    /** getBasicInfo 原始 location 列表 */
    private List<LocationInfo> allLocationInfos = new ArrayList<>();

    private boolean suppressSpinnerCallback = false;
    private boolean isRegistering = false;
    private boolean suppressModuleCallback = false;

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
        tvDeviceId = findViewById(R.id.tv_device_id);
        bindDeviceId();
        containerSessions = findViewById(R.id.container_sessions);
        containerVenuePermissions = findViewById(R.id.container_venue_permissions);
        containerPartitionPermissions = findViewById(R.id.container_partition_permissions);
        containerCertZonePermissions = findViewById(R.id.container_cert_zone_permissions);
        containerParkingPermissions = findViewById(R.id.container_parking_permissions);
        tvLabelPartitionPermission = findViewById(R.id.tv_label_partition_permission);
        tvLabelAreaPermission = findViewById(R.id.tv_label_area_permission);
        tvLabelParkingPermission = findViewById(R.id.tv_label_parking_permission);
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
        venuePermissionChips.clear();
        partitionPermissionChips.clear();
        certZonePermissionChips.clear();

        BasicInfo basicInfo = repository.getBasicInfo();
        if (basicInfo != null) {
            allLocationInfos.clear();
            if (basicInfo.getLocationInfoList() != null) {
                allLocationInfos.addAll(basicInfo.getLocationInfoList());
            }

            if (!TextUtils.isEmpty(currentActiveId)) {
                currentModuleType = AppPreferences.getModuleType(this, currentActiveId);
            }

            rebuildLocationListsForModule();

            suppressSpinnerCallback = true;
            suppressModuleCallback = true;
            setupModuleTypeSpinner();
            setupLocationSpinner();
            setupZoneSpinner();
            suppressModuleCallback = false;
            suppressSpinnerCallback = false;

            applyPermissionSectionVisibility();
            // 权限芯片只来自 matrixAuth 接口缓存，不使用 basicInfo 字典
            renderCachedMatrixAuthPermissions();
            onLocationOrZoneChanged();

            if (switchActivationCheck != null) {
                switchActivationCheck.setChecked(false);
            }
        } else {
            clearPermissionChips();
        }
    }

    /** 按当前人证/车证模块过滤位置与分区 */
    private void rebuildLocationListsForModule() {
        locationList.clear();
        zoneList.clear();
        for (LocationInfo locationInfo : allLocationInfos) {
            if (locationInfo == null
                    || TextUtils.isEmpty(locationInfo.locationName)
                    || TextUtils.isEmpty(locationInfo.locationId)) {
                continue;
            }
            if (!ModuleType.matchesLocation(locationInfo.moduleType, currentModuleType)) {
                continue;
            }
            String locationType = locationInfo.locationType;
            if ("cg".equals(locationType)) {
                locationList.add(locationInfo);
            } else if ("fq".equals(locationType)) {
                zoneList.add(locationInfo);
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

    private void setupModuleTypeSpinner() {
        final String[] types = {ModuleType.LABEL_PERSON, ModuleType.LABEL_VEHICLE};
        int selectedIndex = currentModuleType == ModuleType.VEHICLE ? 1 : 0;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEqpType.setAdapter(adapter);
        spinnerEqpType.setSelection(selectedIndex);
        spinnerEqpType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                if (suppressModuleCallback) {
                    return;
                }
                int newModule = position == 1 ? ModuleType.VEHICLE : ModuleType.PERSON;
                if (newModule == currentModuleType) {
                    return;
                }
                // 切换前先落盘当前模块的位置/分区
                persistCurrentModuleSelection();
                currentModuleType = newModule;
                if (!TextUtils.isEmpty(currentActiveId)) {
                    AppPreferences.setModuleType(EventSettingsActivity.this, currentActiveId, currentModuleType);
                }
                rebuildLocationListsForModule();
                suppressSpinnerCallback = true;
                setupLocationSpinner();
                setupZoneSpinner();
                suppressSpinnerCallback = false;
                applyPermissionSectionVisibility();
                // 先展示当前模块已缓存的 matrixAuth，未同步则重新注册拉取
                renderCachedMatrixAuthPermissions();
                onLocationOrZoneChanged();
                Toast.makeText(EventSettingsActivity.this,
                        "已切换至" + ModuleType.toLabel(currentModuleType)
                                + "（位置/分区与通行权限按模块独立）",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void persistCurrentModuleSelection() {
        if (TextUtils.isEmpty(currentActiveId)) {
            return;
        }
        AppPreferences.setModuleType(this, currentActiveId, currentModuleType);
        if (!TextUtils.isEmpty(selectedLocationId)) {
            AppPreferences.setSelectedLocationId(this, currentActiveId, currentModuleType, selectedLocationId);
        }
        if (!TextUtils.isEmpty(selectedZoneId)) {
            AppPreferences.setSelectedZoneId(this, currentActiveId, currentModuleType, selectedZoneId);
        }
    }

    /**
     * 设置位置下拉选择（场馆类型 locationType="cg"）
     */
    private void setupLocationSpinner() {
        String savedLocationId = AppPreferences.getSelectedLocationId(
                this, currentActiveId, currentModuleType);

        List<String> locationNames = new ArrayList<>();
        locationNames.add("请选择设备所在位置");

        int selectedIndex = 0;
        for (int i = 0; i < locationList.size(); i++) {
            LocationInfo location = locationList.get(i);
            locationNames.add(location.locationName);
            if (TextUtils.equals(location.locationId, savedLocationId)) {
                selectedIndex = i + 1;
            }
        }
        // 当前模块列表里找不到已保存项时，以 Spinner 实际展示为准
        selectedLocationId = selectedIndex > 0
                ? locationList.get(selectedIndex - 1).locationId
                : null;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(adapter);
        spinnerLocation.setSelection(selectedIndex, false);

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
                        persistCurrentModuleSelection();
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
        String savedZoneId = AppPreferences.getSelectedZoneId(
                this, currentActiveId, currentModuleType);

        List<String> zoneNames = new ArrayList<>();
        zoneNames.add("请选择设备所在分区");

        int selectedIndex = 0;
        for (int i = 0; i < zoneList.size(); i++) {
            LocationInfo zone = zoneList.get(i);
            zoneNames.add(zone.locationName);
            if (TextUtils.equals(zone.locationId, savedZoneId)) {
                selectedIndex = i + 1;
            }
        }
        selectedZoneId = selectedIndex > 0
                ? zoneList.get(selectedIndex - 1).locationId
                : null;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, zoneNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZone.setAdapter(adapter);
        spinnerZone.setSelection(selectedIndex, false);

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
                        persistCurrentModuleSelection();
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
     * 位置与分区均选择后：新组合先注册再拉权限；同一组合跳过注册，仍重新请求权限列表。
     */
    private void onLocationOrZoneChanged() {
        if (TextUtils.isEmpty(selectedLocationId) || TextUtils.isEmpty(selectedZoneId)) {
            clearPermissionChips();
            return;
        }
        if (TextUtils.isEmpty(currentActiveId) || isRegistering) {
            return;
        }
        if (AppPreferences.isDeviceAuthSynced(
                this, currentActiveId, currentModuleType, selectedLocationId, selectedZoneId)) {
            Log.d(TAG, "位置/分区未变，跳过注册，刷新权限列表 module=" + currentModuleType);
            refreshMatrixAuthOnly();
            return;
        }
        registerEquipmentAndLoadMatrixAuth();
    }

    /** 已注册过的位置+分区：不重复注册，只重新拉 matrixAuth */
    private void refreshMatrixAuthOnly() {
        String eqpId = ensureDeviceCode();
        if (TextUtils.isEmpty(eqpId)) {
            Toast.makeText(this, "设备编号为空，无法获取权限", Toast.LENGTH_SHORT).show();
            return;
        }
        isRegistering = true;
        Toast.makeText(this,
                "正在刷新" + ModuleType.toLabel(currentModuleType) + "通行权限...",
                Toast.LENGTH_SHORT).show();
        fetchMatrixAuthInfo(eqpId, currentModuleType);
    }

    private void clearPermissionChips() {
        containerVenuePermissions.removeAllViews();
        containerPartitionPermissions.removeAllViews();
        containerCertZonePermissions.removeAllViews();
        if (containerParkingPermissions != null) {
            containerParkingPermissions.removeAllViews();
        }
        venuePermissionChips.clear();
        partitionPermissionChips.clear();
        certZonePermissionChips.clear();
        parkingPermissionChips.clear();
    }

    private void applyPermissionSectionVisibility() {
        boolean vehicle = currentModuleType == ModuleType.VEHICLE;
        int personVis = vehicle ? View.GONE : View.VISIBLE;
        int vehicleVis = vehicle ? View.VISIBLE : View.GONE;
        if (tvLabelPartitionPermission != null) {
            tvLabelPartitionPermission.setVisibility(personVis);
        }
        if (containerPartitionPermissions != null) {
            containerPartitionPermissions.setVisibility(personVis);
        }
        if (tvLabelAreaPermission != null) {
            tvLabelAreaPermission.setVisibility(personVis);
        }
        if (containerCertZonePermissions != null) {
            containerCertZonePermissions.setVisibility(personVis);
        }
        if (tvLabelParkingPermission != null) {
            tvLabelParkingPermission.setVisibility(vehicleVis);
        }
        if (containerParkingPermissions != null) {
            containerParkingPermissions.setVisibility(vehicleVis);
        }
    }

    /** 用当前模块缓存的 matrixAuth 列表渲染权限芯片（全部为设备已有权限） */
    private void renderCachedMatrixAuthPermissions() {
        List<MatrixAuthInfoVo> authList = loadCachedMatrixAuthList();
        bindPermissionChipsFromAuthList(authList);
        DevicePermissionHelper.PermissionSets sets = AppPreferences.getDeviceMatrixAuthCodes(
                this, currentActiveId, currentModuleType);
        Log.d(TAG, "render matrixAuth module=" + currentModuleType
                + " size=" + (authList != null ? authList.size() : 0)
                + " venue=" + sets.venueCodes
                + " park(停车)=" + sets.parkCodes
                + " area(区域)=" + sets.areaCodes
                + " partition(分区)=" + sets.partitionCodes);
    }

    @Nullable
    private List<MatrixAuthInfoVo> loadCachedMatrixAuthList() {
        String json = AppPreferences.getDeviceMatrixAuthJson(
                this, currentActiveId, currentModuleType);
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            Type type = new TypeToken<List<MatrixAuthInfoVo>>() {
            }.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception e) {
            Log.w(TAG, "parse cached matrixAuth json failed", e);
            return null;
        }
    }

    private void bindPermissionChipsFromAuthList(@Nullable List<MatrixAuthInfoVo> authList) {
        clearPermissionChips();
        applyPermissionSectionVisibility();
        if (authList == null || authList.isEmpty()) {
            return;
        }
        addSelectedChips(
                MatrixAuthSelectionHelper.collectVenueLabels(authList),
                containerVenuePermissions, venuePermissionChips);
        if (currentModuleType == ModuleType.VEHICLE) {
            addSelectedChips(
                    MatrixAuthSelectionHelper.collectParkLabels(authList),
                    containerParkingPermissions, parkingPermissionChips);
            return;
        }
        addSelectedChips(
                MatrixAuthSelectionHelper.collectPartitionLabels(authList),
                containerPartitionPermissions, partitionPermissionChips);
        addSelectedChips(
                MatrixAuthSelectionHelper.collectAreaLabels(authList),
                containerCertZonePermissions, certZonePermissionChips);
    }

    private void addSelectedChips(
            LinkedHashMap<String, String> labeledCodes,
            FlowLayout container,
            List<TextView> chipList) {
        if (labeledCodes == null || labeledCodes.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : labeledCodes.entrySet()) {
            String label = !TextUtils.isEmpty(entry.getValue()) ? entry.getValue() : entry.getKey();
            TextView chip = createPermissionChip(label, entry.getKey());
            MatrixAuthSelectionHelper.setChipSelected(chip, true);
            container.addView(chip);
            chipList.add(chip);
        }
    }

    private void registerEquipmentAndLoadMatrixAuth() {
        String eqpId = ensureDeviceCode();
        if (TextUtils.isEmpty(eqpId)) {
            Toast.makeText(this, "设备编号为空，无法注册", Toast.LENGTH_SHORT).show();
            return;
        }

        isRegistering = true;
        final int moduleForRequest = currentModuleType;
        Toast.makeText(this,
                "正在注册" + ModuleType.toLabel(moduleForRequest) + "设备并获取权限...",
                Toast.LENGTH_SHORT).show();

        ApiService apiService = NetworkManager.getInstance().getApiService();
        EquipmentRegisterAo dto = buildEquipmentRegisterAo(eqpId, moduleForRequest);

        apiService.registerEquipment(dto).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call,
                                   @NonNull retrofit2.Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    onRegisterEquipmentFailed(moduleForRequest,
                            ModuleType.toLabel(moduleForRequest) + "设备注册失败");
                    return;
                }
                fetchMatrixAuthInfo(eqpId, moduleForRequest);
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                onRegisterEquipmentFailed(moduleForRequest,
                        ModuleType.toLabel(moduleForRequest) + "设备注册失败：" + t.getMessage());
            }
        });
    }

    /** 注册失败：清空当前模块权限展示与本地缓存，避免沿用上一组位置/分区的权限 */
    private void onRegisterEquipmentFailed(int moduleType, String message) {
        isRegistering = false;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        AppPreferences.clearDeviceMatrixAuth(this, currentActiveId, moduleType);
        if (moduleType == currentModuleType) {
            clearPermissionChips();
        }
    }

    private void fetchMatrixAuthInfo(String eqpId, int moduleType) {
        ApiService apiService = NetworkManager.getInstance().getApiService();
        // 人证走 getMatrixAuthInfoList，车证走 getCarMatrixAuthList；入参返参与后续处理相同
        retrofit2.Call<ApiResponse<java.util.List<MatrixAuthInfoVo>>> authCall =
                moduleType == ModuleType.VEHICLE
                        ? apiService.getCarMatrixAuthList(currentActiveId, eqpId)
                        : apiService.getMatrixAuthInfoList(currentActiveId, eqpId);
        authCall.enqueue(new retrofit2.Callback<ApiResponse<java.util.List<MatrixAuthInfoVo>>>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<ApiResponse<java.util.List<MatrixAuthInfoVo>>> call,
                                           @NonNull retrofit2.Response<ApiResponse<java.util.List<MatrixAuthInfoVo>>> response) {
                        isRegistering = false;
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(EventSettingsActivity.this,
                                    "获取设备权限失败", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ApiResponse<java.util.List<MatrixAuthInfoVo>> apiResponse = response.body();
                        if (apiResponse.getCode() != 200) {
                            Toast.makeText(EventSettingsActivity.this,
                                    "获取设备权限失败：" + apiResponse.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        java.util.List<MatrixAuthInfoVo> authList = apiResponse.getData();
                        DevicePermissionHelper.PermissionSets sets =
                                DevicePermissionHelper.fromMatrixAuthDtoList(authList);
                        AppPreferences.setDeviceMatrixAuthCodes(
                                EventSettingsActivity.this, currentActiveId, moduleType, sets);
                        try {
                            AppPreferences.setDeviceMatrixAuthJson(
                                    EventSettingsActivity.this, currentActiveId, moduleType,
                                    new Gson().toJson(authList));
                        } catch (Exception e) {
                            Log.w(TAG, "cache matrixAuth json failed", e);
                        }
                        AppPreferences.setDevicePermissionConfigured(
                                EventSettingsActivity.this, currentActiveId, moduleType,
                                moduleType == ModuleType.VEHICLE
                                        ? sets.hasCarConfigured()
                                        : sets.hasPersonConfigured());
                        if (!TextUtils.isEmpty(selectedLocationId) && !TextUtils.isEmpty(selectedZoneId)) {
                            AppPreferences.setDeviceAuthSynced(
                                    EventSettingsActivity.this, currentActiveId, moduleType,
                                    selectedLocationId, selectedZoneId);
                        }
                        if (moduleType == currentModuleType) {
                            bindPermissionChipsFromAuthList(authList);
                            Log.d(TAG, "matrixAuth applied module=" + moduleType
                                    + " size=" + (authList != null ? authList.size() : 0)
                                    + " venue=" + sets.venueCodes
                                    + " area(区域)=" + sets.areaCodes
                                    + " partition(分区)=" + sets.partitionCodes);
                        }
                        Toast.makeText(EventSettingsActivity.this,
                                "已加载" + ModuleType.toLabel(moduleType) + "通行权限",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull retrofit2.Call<ApiResponse<java.util.List<MatrixAuthInfoVo>>> call,
                                          @NonNull Throwable t) {
                        isRegistering = false;
                        Toast.makeText(EventSettingsActivity.this,
                                "获取设备权限失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private EquipmentRegisterAo buildEquipmentRegisterAo(String eqpId, int moduleType) {
        EquipmentRegisterAo dto = new EquipmentRegisterAo();
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
        dto.eqpName = "手持设备-" + ModuleType.toLabel(moduleType);
        dto.eqpState = 0;
        // 后台约定：人证 / 车证 分开注册
        dto.eqpType = ModuleType.toLabel(moduleType);
        dto.locationID = selectedLocationId;
        dto.zoneLocationID = selectedZoneId;
        return dto;
    }

    private String ensureDeviceCode() {
        return AppPreferences.ensureDeviceCode(this);
    }

    /** 展示本机业务设备 ID（注册 / 拉权限 / 核验上报共用） */
    private void bindDeviceId() {
        if (tvDeviceId == null) {
            return;
        }
        String eqpId = ensureDeviceCode();
        tvDeviceId.setText(TextUtils.isEmpty(eqpId) ? "--" : eqpId);
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
        checkBox.setClickable(false);
        checkBox.setFocusable(false);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dpToPx(8);
        params.bottomMargin = dpToPx(8);
        checkBox.setLayoutParams(params);
        return checkBox;
    }

    /** 权限列表芯片：选中为蓝底白字，不依赖系统勾选框渲染 */
    private TextView createPermissionChip(String text, String dictCode) {
        TextView chip = new TextView(this);
        chip.setText(text);
        if (!TextUtils.isEmpty(dictCode)) {
            chip.setTag(dictCode);
        }
        chip.setTextSize(14f);
        chip.setClickable(false);
        chip.setFocusable(false);
        int padH = dpToPx(10);
        int padV = dpToPx(6);
        chip.setPadding(padH, padV, padH, padV);
        MatrixAuthSelectionHelper.setChipSelected(chip, false);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dpToPx(8);
        params.bottomMargin = dpToPx(8);
        chip.setLayoutParams(params);
        return chip;
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
    private Set<String> getSelectedChipTexts(List<TextView> chips) {
        Set<String> selected = new java.util.HashSet<>();
        for (TextView chip : chips) {
            if (chip != null && chip.isSelected()) {
                selected.add(chip.getText().toString());
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
        if (TextUtils.isEmpty(selectedLocationId) || TextUtils.isEmpty(selectedZoneId)) {
            Toast.makeText(this,
                    "请同时选择" + ModuleType.toLabel(currentModuleType)
                            + "的设备所在位置和设备所在分区（两项均必选）",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        persistCurrentModuleSelection();

        Intent result = new Intent();
        result.putStringArrayListExtra("selected_sessions", new ArrayList<>(getCurrentSelectedSessions()));
        result.putStringArrayListExtra("selected_venues", new ArrayList<String>());
        result.putStringArrayListExtra("selected_areas", new ArrayList<String>());
        result.putStringArrayListExtra("selected_cert_zones", new ArrayList<String>());
        result.putExtra("selected_location_id", selectedLocationId);
        result.putExtra("selected_zone_id", selectedZoneId);
        result.putExtra("module_type", currentModuleType);
        result.putExtra("eqp_type", ModuleType.toLabel(currentModuleType));
        result.putExtra("activation_check_enabled", false);
        result.putExtra("selected_sub_unit", (String) null);
        boolean permConfigured = AppPreferences.isDevicePermissionConfigured(
                this, currentActiveId, currentModuleType);
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
        retrofit2.Call<ApiResponse<java.util.List<ActiveUserBaseVo>>> call =
                apiService.getActiveUser(currentActiveId, null);

        call.enqueue(new retrofit2.Callback<ApiResponse<List<ActiveUserBaseVo>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveUserBaseVo>>> call,
                                   @NonNull retrofit2.Response<ApiResponse<java.util.List<ActiveUserBaseVo>>> response) {
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        handleSyncFailure("数据同步失败：服务器错误");
                        return;
                    }

                    ApiResponse<java.util.List<ActiveUserBaseVo>> apiResponse = response.body();
                    if (apiResponse == null || apiResponse.getCode() != 200) {
                        handleSyncFailure("数据同步失败：" + (apiResponse != null ? apiResponse.getMessage() : "接口返回异常"));
                        return;
                    }

                    try {
                        java.util.List<ActiveUserBaseVo> dataList = apiResponse.getData();
                        JSONArray dataArray = new JSONArray();
                        if (dataList != null) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            for (ActiveUserBaseVo dto : dataList) {
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
            public void onFailure(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveUserBaseVo>>> call,
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

        // 调用接口获取该活动的基础信息（需传设备 id）
        String eqpId = ensureDeviceCode();
        ApiService apiService = NetworkManager.getInstance().getApiService();
        retrofit2.Call<ApiResponse<com.largeevent.management.network.dto.BasicInfoVo>> call =
                apiService.getBasicInfo(eventCode, eqpId);

        call.enqueue(new retrofit2.Callback<ApiResponse<com.largeevent.management.network.dto.BasicInfoVo>>() {
            @Override
            public void onResponse(
                    @NonNull retrofit2.Call<ApiResponse<com.largeevent.management.network.dto.BasicInfoVo>> call,
                    @NonNull retrofit2.Response<ApiResponse<com.largeevent.management.network.dto.BasicInfoVo>> response) {
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        Toast.makeText(EventSettingsActivity.this,
                                "基础信息获取失败：服务器错误", Toast.LENGTH_SHORT).show();
                        currentEventInfo = repository.getEventInfo();
                        renderEventInfo();
                        return;
                    }
                    ApiResponse<com.largeevent.management.network.dto.BasicInfoVo> apiResponse = response.body();
                    if (apiResponse == null || !apiResponse.isSuccess() || apiResponse.getData() == null) {
                        Toast.makeText(EventSettingsActivity.this,
                                "基础信息获取失败：" + (apiResponse != null
                                        ? apiResponse.getMessage() : "接口返回异常"),
                                Toast.LENGTH_SHORT).show();
                        currentEventInfo = repository.getEventInfo();
                        renderEventInfo();
                        return;
                    }
                    try {
                        repository.saveBaseInfo(baseUrl, apiResponse.getData());
                        currentEventInfo = repository.getEventInfo();
                        if (currentEventInfo == null) {
                            currentEventInfo = new EventInfo(activeName, new ArrayList<>(),
                                    new ArrayList<>(), new ArrayList<>());
                        }
                        renderEventInfo();
                    } catch (Exception ex) {
                        Toast.makeText(EventSettingsActivity.this,
                                "基础信息保存失败：" + ex.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        currentEventInfo = repository.getEventInfo();
                        renderEventInfo();
                    }
                });
            }

            @Override
            public void onFailure(
                    @NonNull retrofit2.Call<ApiResponse<com.largeevent.management.network.dto.BasicInfoVo>> call,
                    @NonNull Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(EventSettingsActivity.this,
                            "基础信息获取失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
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