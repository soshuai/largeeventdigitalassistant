package com.largeevent.management.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.largeevent.management.DataDetailEntryActivity;
import com.largeevent.management.EventSettingsActivity;
import com.largeevent.management.R;
import com.largeevent.management.data.ActiveInfoMapper;
import com.largeevent.management.data.ActiveUserParser;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.network.dto.ActiveInfoDTO;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.EventInfo;
import com.largeevent.management.network.CustomHttpLogger;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.ApiResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SettingsFragment extends Fragment {

    private static final int REQUEST_CODE_EVENT_SETTINGS = 1001;

    private InitializationRepository repository;
    private EditText etServer;
    private Button btnTestConnection;
    private Button btnSaveSetting;
    private Spinner spinnerEvent;
    private View btnRefreshActivities;
    private List<BasicInfo.ActiveModel> activeModelList = new ArrayList<>();
    private String selectedActiveId = null;

    private EventInfo currentEventInfo;

    // 保存从 EventSettingsActivity 返回的选中数据
    private Set<String> pendingSelectedSessions = new HashSet<>();
    private Set<String> pendingSelectedVenues = new HashSet<>();
    private Set<String> pendingSelectedAreas = new HashSet<>();
    private Set<String> pendingSelectedCertZones = new HashSet<>();
    private String pendingSelectedLocationId = null;
    private String pendingSelectedZoneId = null;
    
    // 保存当前活动ID用于设备注册
    private String currentActiveIdForSave = null;
    
    // 防止重复点击标志

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new InitializationRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etServer = view.findViewById(R.id.et_setting_server);
        btnTestConnection = view.findViewById(R.id.btn_test_setting_server);
        btnSaveSetting = view.findViewById(R.id.btn_save_setting_server);
        spinnerEvent = view.findViewById(R.id.spinner_event);
        btnRefreshActivities = view.findViewById(R.id.btn_refresh_activities);
        Button btnViewEventDetail = view.findViewById(R.id.btn_view_event_detail);

        String savedServer = AppPreferences.getServerUrl(requireContext());
        etServer.setText(savedServer);
        NetworkManager.getInstance().setBaseUrl(savedServer);

        // 加载活动列表
        loadActiveModelList();

        currentEventInfo = repository.getEventInfo();
        
        // 初始化 pending 权限变量，读取当前已保存的权限，避免未进入EventSettingsActivity时被置空
        // 使用当前选中的活动ID读取权限，确保活动隔离
        String currentActiveId = AppPreferences.getLastActiveId(requireContext());
        pendingSelectedSessions = new HashSet<>(AppPreferences.getSelectedSessions(requireContext(), currentActiveId));
        pendingSelectedVenues = new HashSet<>(AppPreferences.getSelectedVenuePermissions(requireContext(), currentActiveId));
        pendingSelectedAreas = new HashSet<>(AppPreferences.getSelectedAreaPermissions(requireContext(), currentActiveId));
        pendingSelectedCertZones = new HashSet<>(AppPreferences.getSelectedCertZonePermissions(requireContext(), currentActiveId));
        pendingSelectedLocationId = AppPreferences.getSelectedLocationId(requireContext(), currentActiveId);
        pendingSelectedZoneId = AppPreferences.getSelectedZoneId(requireContext(), currentActiveId);

        btnTestConnection.setOnClickListener(v -> testServerConnection());
        btnSaveSetting.setOnClickListener(v -> saveSettings());
        btnViewEventDetail.setOnClickListener(v -> openEventSettings());
        btnRefreshActivities.setOnClickListener(v -> refreshActivitiesList());
    }

    /**
     * 加载活动列表并初始化Spinner
     */
    private void loadActiveModelList() {
        // 从 repository 中获取活动列表
        List<BasicInfo.ActiveModel> activeList = repository.getActiveList();
        if (activeList != null && !activeList.isEmpty()) {
            activeModelList = new ArrayList<>(activeList);
        } else {
            // 如果活动列表为空，尝试从 BasicInfo 中获取（向后兼容）
            BasicInfo basicInfo = repository.getBasicInfo();
            if (basicInfo != null) {
                activeModelList = new ArrayList<>(basicInfo.getActiveModels());
            } else {
                activeModelList = new ArrayList<>();
            }
        }

        // 准备Spinner数据
        List<String> activeNames = new ArrayList<>();
        if (activeModelList.isEmpty()) {
            activeNames.add("未设置活动");
        } else {
            for (BasicInfo.ActiveModel model : activeModelList) {
                if (model != null && !TextUtils.isEmpty(model.activeName)) {
                    activeNames.add(model.activeName);
                } else {
                    activeNames.add("未命名活动");
                }
            }
        }

        // 设置Spinner适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, activeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEvent.setAdapter(adapter);

        // 读取上次保存的活动ID，如果没有则默认选中第一个
        String savedActiveId = AppPreferences.getLastActiveId(requireContext());
        int selectedPosition = 0;

        if (!TextUtils.isEmpty(savedActiveId) && !activeModelList.isEmpty()) {
            for (int i = 0; i < activeModelList.size(); i++) {
                BasicInfo.ActiveModel model = activeModelList.get(i);
                if (model != null && savedActiveId.equals(model.id)) {
                    selectedPosition = i;
                    break;
                }
            }
        }

        // 设置选中项
        spinnerEvent.setSelection(selectedPosition);

        // 保存当前选中的活动ID
        if (!activeModelList.isEmpty() && activeModelList.get(selectedPosition) != null) {
            selectedActiveId = activeModelList.get(selectedPosition).id;
        }

        // 设置选项改变监听器
        spinnerEvent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < activeModelList.size() && activeModelList.get(position) != null) {
                    String newActiveId = activeModelList.get(position).id;
                    // 如果活动ID发生变化，重新加载该活动的权限
                    if (!TextUtils.equals(selectedActiveId, newActiveId)) {
                        selectedActiveId = newActiveId;
                        // 重新加载当前活动的权限到 pending 变量
                        loadPermissionsForCurrentActive();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不处理
            }
        });
    }

    /**
     * 加载当前选中活动的权限
     */
    private void loadPermissionsForCurrentActive() {
        if (TextUtils.isEmpty(selectedActiveId)) {
            pendingSelectedSessions = new HashSet<>();
            pendingSelectedVenues = new HashSet<>();
            pendingSelectedAreas = new HashSet<>();
            pendingSelectedCertZones = new HashSet<>();
            pendingSelectedLocationId = null;
            pendingSelectedZoneId = null;
        } else {
            pendingSelectedSessions = new HashSet<>(AppPreferences.getSelectedSessions(requireContext(), selectedActiveId));
            pendingSelectedVenues = new HashSet<>(AppPreferences.getSelectedVenuePermissions(requireContext(), selectedActiveId));
            pendingSelectedAreas = new HashSet<>(AppPreferences.getSelectedAreaPermissions(requireContext(), selectedActiveId));
            pendingSelectedCertZones = new HashSet<>(AppPreferences.getSelectedCertZonePermissions(requireContext(), selectedActiveId));
            pendingSelectedLocationId = AppPreferences.getSelectedLocationId(requireContext(), selectedActiveId);
            pendingSelectedZoneId = AppPreferences.getSelectedZoneId(requireContext(), selectedActiveId);
        }
    }

    private void testServerConnection() {
        String raw = etServer.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            Toast.makeText(requireContext(), "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }
        final String normalized = normalizeBaseUrl(raw);
        setTestButtonLoading(true);
        
        // 使用Java原生URL进行HEAD请求测试连接
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(normalized);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                boolean success = responseCode >= 200 && responseCode < 400;
                connection.disconnect();
                
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setTestButtonLoading(false);
                    if (success) {
                        Toast.makeText(requireContext(), "连接成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "连接失败，状态码：" + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setTestButtonLoading(false);
                    Toast.makeText(requireContext(), "连接失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setTestButtonLoading(boolean loading) {
        btnTestConnection.setEnabled(!loading);
        btnTestConnection.setText(loading ? "测试中..." : "测试连接");
    }

    private void saveSettings() {
        String raw = etServer.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            Toast.makeText(requireContext(), "服务器地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        String normalized = normalizeBaseUrl(raw);
        etServer.setText(normalized);

        // 读取旧地址并判断是否变化
        String oldUrl = AppPreferences.getServerUrl(requireContext());
        boolean urlChanged = !TextUtils.equals(oldUrl, normalized);

        // 保存服务器地址
        AppPreferences.setServerUrl(requireContext(), normalized);
        NetworkManager.getInstance().setBaseUrl(normalized);

        // 保存选中的活动ID
        if (!TextUtils.isEmpty(selectedActiveId)) {
            AppPreferences.setLastActiveId(requireContext(), selectedActiveId);
        }

        // 保存权限选择（无论是否为空都要保存，空集合代表全部取消选中）
        // 使用当前选中的活动ID进行保存，确保活动隔离
        String activeIdForSave = !TextUtils.isEmpty(selectedActiveId) ? selectedActiveId : AppPreferences.getLastActiveId(requireContext());
        currentActiveIdForSave = activeIdForSave;
        AppPreferences.setSelectedSessions(requireContext(), activeIdForSave, pendingSelectedSessions);
        AppPreferences.setSelectedVenuePermissions(requireContext(), activeIdForSave, pendingSelectedVenues);
        AppPreferences.setSelectedAreaPermissions(requireContext(), activeIdForSave, pendingSelectedAreas);
        AppPreferences.setSelectedCertZonePermissions(requireContext(), activeIdForSave, pendingSelectedCertZones);
        
        // 保存位置和分区ID
        if (!TextUtils.isEmpty(pendingSelectedLocationId)) {
            AppPreferences.setSelectedLocationId(requireContext(), activeIdForSave, pendingSelectedLocationId);
        }
        if (!TextUtils.isEmpty(pendingSelectedZoneId)) {
            AppPreferences.setSelectedZoneId(requireContext(), activeIdForSave, pendingSelectedZoneId);
        }
        if (!TextUtils.isEmpty(pendingSelectedLocationId) && !TextUtils.isEmpty(pendingSelectedZoneId)) {
            AppPreferences.setDeviceAuthSynced(requireContext(), activeIdForSave,
                    pendingSelectedLocationId, pendingSelectedZoneId);
        }
        AppPreferences.setDevicePermissionConfigured(requireContext(), activeIdForSave,
                !pendingSelectedVenues.isEmpty()
                        || !pendingSelectedAreas.isEmpty()
                        || !pendingSelectedCertZones.isEmpty());

        // 通知HomeFragment更新自动初始化的服务器地址
        notifyHomeFragmentServerUrlChanged(normalized);

        // 位置/分区/权限已在活动设置页处理；此处仅持久化配置
        if (urlChanged) {
            Toast.makeText(requireContext(), "服务器地址已变更，正在刷新基础信息", Toast.LENGTH_SHORT).show();
            fetchBasicInfo(normalized);
        } else {
            Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show();
        }
    }

    // 添加通知HomeFragment服务器地址变更的方法
    private void notifyHomeFragmentServerUrlChanged(String newServerUrl) {
        // 通过SharedPreferences发送广播，通知HomeFragment更新服务器地址
        SharedPreferences prefs = requireContext().getSharedPreferences("app_events", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("server_url_updated", newServerUrl);
        editor.putLong("server_url_update_time", System.currentTimeMillis());
        editor.apply();
    }

    // 服务器地址变更后，重新获取基础信息
    private void fetchBasicInfo(String baseUrl) {
        // 第一步：先获取活动列表
        com.largeevent.management.network.ApiService apiService = NetworkManager.getInstance().getApiService();
        retrofit2.Call<ApiResponse<java.util.List<ActiveInfoDTO>>> call = apiService.getActiveInfo();
        
        call.enqueue(new retrofit2.Callback<ApiResponse<java.util.List<ActiveInfoDTO>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveInfoDTO>>> call, 
                                   @NonNull retrofit2.Response<ApiResponse<java.util.List<ActiveInfoDTO>>> response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), "活动列表获取失败：服务器错误", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    ApiResponse<java.util.List<ActiveInfoDTO>> apiResponse = response.body();
                    if (apiResponse == null || apiResponse.getCode() != 200) {
                        Toast.makeText(requireContext(), "活动列表获取失败：" + 
                                (apiResponse != null ? apiResponse.getMessage() : "接口返回异常"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    java.util.List<ActiveInfoDTO> activeList = apiResponse.getData();
                    if (activeList == null || activeList.isEmpty()) {
                        Toast.makeText(requireContext(), "活动列表为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 保存活动列表
                    try {
                        repository.saveActiveList(baseUrl, ActiveInfoMapper.toJsonArrayString(activeList));
                    } catch (Exception ex) {
                        Toast.makeText(requireContext(), "活动列表保存失败：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 获取第一个活动的 eventCode
                    BasicInfo.ActiveModel firstActive = ActiveInfoMapper.toActiveModel(activeList.get(0));
                    if (firstActive != null) {
                        String eventCode = firstActive.id;  // 使用活动ID作为eventCode
                        if (!TextUtils.isEmpty(eventCode)) {
                            // 保存活动ID
                            AppPreferences.setLastActiveId(requireContext(), eventCode);
                            // 第二步：获取基础信息
                            fetchBasicInfoWithEventCode(baseUrl, eventCode);
                        } else {
                            Toast.makeText(requireContext(), "活动ID为空", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveInfoDTO>>> call, 
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "活动列表获取失败：" + t.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void fetchBasicInfoWithEventCode(String baseUrl, String eventCode) {
        com.largeevent.management.network.ApiService apiService = NetworkManager.getInstance().getApiService();
        retrofit2.Call<ResponseBody> call = apiService.getBasicInfo(eventCode);
        
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call, 
                                   @NonNull retrofit2.Response<ResponseBody> response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), "基础信息获取失败：服务器错误", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    try {
                        String respText = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(respText);
                        if (json.optInt("code", -1) != 200) {
                            Toast.makeText(requireContext(), "基础信息获取失败：" + json.optString("message", "接口返回异常"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 保存基础信息
                        try {
                            repository.saveBaseInfo(baseUrl, respText);
                        } catch (Exception ex) {
                            Toast.makeText(requireContext(), "基础信息保存失败：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 重新加载活动列表
                        loadActiveModelList();
                        // 刷新当前活动显示
                        currentEventInfo = repository.getEventInfo();
                        Toast.makeText(requireContext(), "基础信息已刷新", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "基础信息处理失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "基础信息获取失败：" + t.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void openEventSettings() {
        Intent intent = new Intent(requireContext(), EventSettingsActivity.class);
        // 传入当前选择的活动ID，确保权限展示与保存按活动隔离
        if (!TextUtils.isEmpty(selectedActiveId)) {
            intent.putExtra("active_id", selectedActiveId);
        }
        startActivityForResult(intent, REQUEST_CODE_EVENT_SETTINGS);
    }

    private String normalizeBaseUrl(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }
        String url = raw.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
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
     * 刷新活动列表
     */
    private void refreshActivitiesList() {
        String baseUrl = AppPreferences.getServerUrl(requireContext());
        if (TextUtils.isEmpty(baseUrl)) {
            Toast.makeText(requireContext(), "请先设置服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用刷新按钮，防止重复点击
        btnRefreshActivities.setEnabled(false);
        
        com.largeevent.management.network.ApiService apiService = NetworkManager.getInstance().getApiService();
        retrofit2.Call<ApiResponse<java.util.List<ActiveInfoDTO>>> call = apiService.getActiveInfo();
        
        call.enqueue(new retrofit2.Callback<ApiResponse<java.util.List<ActiveInfoDTO>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveInfoDTO>>> call,
                                   @NonNull retrofit2.Response<ApiResponse<java.util.List<ActiveInfoDTO>>> response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnRefreshActivities.setEnabled(true);
                    
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(), "活动列表刷新失败：服务器错误", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    ApiResponse<java.util.List<ActiveInfoDTO>> apiResponse = response.body();
                    if (apiResponse == null || apiResponse.getCode() != 200) {
                        Toast.makeText(requireContext(), "活动列表刷新失败：" + 
                                (apiResponse != null ? apiResponse.getMessage() : "接口返回异常"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    java.util.List<ActiveInfoDTO> activeList = apiResponse.getData();
                    if (activeList == null || activeList.isEmpty()) {
                        Toast.makeText(requireContext(), "活动列表为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 保存活动列表
                    try {
                        repository.saveActiveList(baseUrl, ActiveInfoMapper.toJsonArrayString(activeList));
                        activeModelList = new ArrayList<>(ActiveInfoMapper.toActiveModels(activeList));
                        loadActiveModelList();
                        Toast.makeText(requireContext(), "活动列表已刷新", Toast.LENGTH_SHORT).show();
                    } catch (Exception ex) {
                        Toast.makeText(requireContext(), "活动列表保存失败：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiResponse<java.util.List<ActiveInfoDTO>>> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnRefreshActivities.setEnabled(true);
                    Toast.makeText(requireContext(), "活动列表刷新失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EVENT_SETTINGS && resultCode == android.app.Activity.RESULT_OK && data != null) {
            // 从 EventSettingsActivity 返回的选中数据
            ArrayList<String> sessions = data.getStringArrayListExtra("selected_sessions");
            ArrayList<String> venues = data.getStringArrayListExtra("selected_venues");
            ArrayList<String> areas = data.getStringArrayListExtra("selected_areas");
            ArrayList<String> certZones = data.getStringArrayListExtra("selected_cert_zones");
            String locationId = data.getStringExtra("selected_location_id");
            String zoneId = data.getStringExtra("selected_zone_id");

            // 保存到临时变量，等待用户点击“保存设置”按钮
            pendingSelectedSessions = sessions != null ? new HashSet<>(sessions) : new HashSet<>();
            pendingSelectedVenues = venues != null ? new HashSet<>(venues) : new HashSet<>();
            pendingSelectedAreas = areas != null ? new HashSet<>(areas) : new HashSet<>();
            pendingSelectedCertZones = certZones != null ? new HashSet<>(certZones) : new HashSet<>();
            pendingSelectedLocationId = locationId;
            pendingSelectedZoneId = zoneId;

            String activeId = AppPreferences.getLastActiveId(requireContext());
            if (!TextUtils.isEmpty(activeId)) {
                AppPreferences.setActivationCheckEnabled(requireContext(), activeId,
                        data.getBooleanExtra("activation_check_enabled", false));
                AppPreferences.setSelectedSubUnitName(requireContext(), activeId,
                        data.getStringExtra("selected_sub_unit"));
                String eqpType = data.getStringExtra("eqp_type");
                if (!TextUtils.isEmpty(eqpType)) {
                    AppPreferences.setEqpType(requireContext(), activeId, eqpType);
                }
                AppPreferences.setDevicePermissionConfigured(requireContext(), activeId,
                        data.getBooleanExtra("device_perm_configured", false)
                                || !pendingSelectedVenues.isEmpty()
                                || !pendingSelectedAreas.isEmpty()
                                || !pendingSelectedCertZones.isEmpty());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.close();
        }
    }
}

