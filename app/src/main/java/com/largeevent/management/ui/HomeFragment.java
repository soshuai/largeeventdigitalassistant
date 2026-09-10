package com.largeevent.management.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.largeevent.management.MainActivity;
import com.largeevent.management.QueryCertActivity;
import com.largeevent.management.R;
import com.largeevent.management.RecordActivity;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.ApiResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private InitializationRepository repository;

    private View introContainer;
    private View initCard;
    private View progressLayout;
    private View errorLayout;
    private View successLayout;
    private EditText etServerUrl;
    private Button btnTestConnection;
    private Button btnConfirm;
    private ProgressBar progressBar;
    private TextView tvProgressPercent;
    private TextView tvProgressTip;
    private TextView tvErrorTitle;
    private Button btnModifyConnection;
    private Button btnRetryDownload;
    private View cardPersonVerify;
    private View cardVehicleVerify;
    private View cardRecord;
    private View cardQueryCert;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // 添加定时任务相关的变量
    private static final long AUTO_INIT_INTERVAL = 10 * 60 * 1000; // 10分钟
    private Handler autoInitHandler;
    private Runnable autoInitRunnable;
    
    // 添加SharedPreferences引用
    private SharedPreferences appEventsPrefs;

    private String lastTestedUrl = "";
    private boolean lastTestSuccess = false;
    private String initializingUrl = "";
    private int currentProgress = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = new InitializationRepository(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化定时任务
        initAutoInitialization();
        
        // 注册SharedPreferences监听器
        appEventsPrefs = requireContext().getSharedPreferences("app_events", Context.MODE_PRIVATE);
        appEventsPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        introContainer = view.findViewById(R.id.layout_intro);
        initCard = view.findViewById(R.id.card_initialize);
        progressLayout = view.findViewById(R.id.layout_progress);
        errorLayout = view.findViewById(R.id.layout_error);
        successLayout = view.findViewById(R.id.layout_success);
        etServerUrl = view.findViewById(R.id.et_server_url);
        btnTestConnection = view.findViewById(R.id.btn_test_connection);
        btnConfirm = view.findViewById(R.id.btn_confirm_initialize);
        progressBar = view.findViewById(R.id.progress_download);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        tvProgressTip = view.findViewById(R.id.tv_progress_tip);
        tvErrorTitle = view.findViewById(R.id.tv_error_title);
        btnModifyConnection = view.findViewById(R.id.btn_modify_connection);
        btnRetryDownload = view.findViewById(R.id.btn_retry_download);
        cardPersonVerify = view.findViewById(R.id.card_person_verify);
        cardVehicleVerify = view.findViewById(R.id.card_vehicle_verify);
        cardRecord = view.findViewById(R.id.card_record);
        cardQueryCert = view.findViewById(R.id.card_query_cert);

        Button startInitButton = view.findViewById(R.id.btn_start_initialize);
        startInitButton.setOnClickListener(v -> showFormState());

        btnTestConnection.setOnClickListener(v -> testConnection());
        btnConfirm.setOnClickListener(v -> confirmInitialization());
        btnModifyConnection.setOnClickListener(v -> showFormState());
        btnRetryDownload.setOnClickListener(v -> retryInitialization());
        cardPersonVerify.setOnClickListener(v -> navigateToTab(R.id.nav_person_verify));
        cardVehicleVerify.setOnClickListener(v -> navigateToTab(R.id.nav_vehicle_verify));
        cardRecord.setOnClickListener(v -> openRecordActivity());
        cardQueryCert.setOnClickListener(v -> openQueryCertActivity());

        String savedServerUrl = AppPreferences.getServerUrl(requireContext());
        if (!TextUtils.isEmpty(savedServerUrl)) {
            etServerUrl.setText(savedServerUrl);
             NetworkManager.getInstance().setBaseUrl(savedServerUrl);
        }
        
        // 检查是否已经初始化完成，如果完成则直接显示成功状态
        checkInitializationStatus();
    }

    // 添加检查初始化状态的方法
    private void checkInitializationStatus() {
        String savedServerUrl = AppPreferences.getServerUrl(requireContext());
        if (!TextUtils.isEmpty(savedServerUrl)) {
            // 如果已经有保存的服务器URL，说明可能已经初始化过
            // 检查数据库中是否有数据
            if (repository.hasData()) {
                showSuccessState();
                // 启动定时初始化任务
                startAutoInitialization();
            }
        }
    }

    // 初始化自动初始化功能
    private void initAutoInitialization() {
        autoInitHandler = new Handler(Looper.getMainLooper());
        autoInitRunnable = new Runnable() {
            @Override
            public void run() {
                performAutoInitialization();
                // 10分钟后再次执行
                autoInitHandler.postDelayed(this, AUTO_INIT_INTERVAL);
            }
        };
    }

    // 启动自动初始化
    private void startAutoInitialization() {
        if (autoInitHandler != null && autoInitRunnable != null) {
            // 先移除之前的任务，避免重复
            autoInitHandler.removeCallbacks(autoInitRunnable);
            // 30分钟后执行自动初始化
            autoInitHandler.postDelayed(autoInitRunnable, AUTO_INIT_INTERVAL);
        }
    }

    // 执行自动初始化
    private void performAutoInitialization() {
        String savedServerUrl = AppPreferences.getServerUrl(requireContext());
        if (!TextUtils.isEmpty(savedServerUrl)) {
            // 自动初始化：获取活动列表
            fetchActiveList(savedServerUrl);
        }
    }

    private void showIntroState() {
        introContainer.setVisibility(View.VISIBLE);
        initCard.setVisibility(View.GONE);
        progressLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.GONE);
    }

    private void showFormState() {
        introContainer.setVisibility(View.GONE);
        initCard.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.GONE);
    }

    private void showProgressState() {
        introContainer.setVisibility(View.GONE);
        initCard.setVisibility(View.GONE);
        progressLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.GONE);
    }

    private void showErrorState(@NonNull String message) {
        introContainer.setVisibility(View.GONE);
        initCard.setVisibility(View.GONE);
        progressLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        successLayout.setVisibility(View.GONE);
        if (tvErrorTitle != null) {
            tvErrorTitle.setText(TextUtils.isEmpty(message) ? "初始化失败" : message);
        }
    }

    private void showSuccessState() {
        introContainer.setVisibility(View.GONE);
        initCard.setVisibility(View.GONE);
        progressLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.VISIBLE);
    }

    private void navigateToTab(int navId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(navId);
        }
    }

    private void openRecordActivity() {
        Intent intent = new Intent(requireContext(), RecordActivity.class);
        startActivity(intent);
    }

    private void openQueryCertActivity() {
        Intent intent = new Intent(requireContext(), QueryCertActivity.class);
        startActivity(intent);
    }

    private void retryInitialization() {
        if (TextUtils.isEmpty(initializingUrl)) {
            Toast.makeText(requireContext(), "请先输入地址并测试连接", Toast.LENGTH_SHORT).show();
            showFormState();
            return;
        }
        startInitializationDownload(initializingUrl);
    }

    private void testConnection() {
        String rawUrl = etServerUrl.getText().toString().trim();
        if (TextUtils.isEmpty(rawUrl)) {
            Toast.makeText(requireContext(), "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        final String targetUrl = normalizeUrl(rawUrl);
        toggleTestButton(false);
        lastTestedUrl = targetUrl;

        // 使用 NetworkManager 测试连接
        executorService.execute(() -> {
            try {
                // 简单的 HTTP HEAD 请求测试连接
                java.net.URL url = new java.net.URL(targetUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                boolean success = responseCode >= 200 && responseCode < 400;
                String message = success ? "连接成功" : "连接失败，状态码：" + responseCode;
                connection.disconnect();
                
                boolean finalSuccess = success;
                String finalMessage = message;
                if (!isAdded()) {
                    return;
                }
                NetworkManager.getInstance().setBaseUrl(targetUrl);
                AppPreferences.setServerUrl(requireContext(), targetUrl);

                requireActivity().runOnUiThread(() -> {
                    toggleTestButton(true);
                    lastTestSuccess = finalSuccess;
                    showInfoDialog("提示", finalMessage);
                });
            } catch (Exception e) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    toggleTestButton(true);
                    lastTestSuccess = false;
                    showInfoDialog("提示", "连接失败：" + e.getMessage());
                });
            }
        });
    }

    private void confirmInitialization() {
        String rawUrl = etServerUrl.getText().toString().trim();
        if (TextUtils.isEmpty(rawUrl)) {
            Toast.makeText(requireContext(), "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }
        String normalizedUrl = normalizeUrl(rawUrl);

        if (!TextUtils.equals(lastTestedUrl, normalizedUrl)) {
            lastTestSuccess = false;
            lastTestedUrl = "";
            showInfoDialog("提示", "请先测试当前地址的连接情况");
            return;
        }

        if (!lastTestSuccess) {
            showInfoDialog("提示", "地址有误请更换地址");
            return;
        }

        startInitializationDownload(normalizedUrl);
    }

    private void startInitializationDownload(String baseUrl) {
        initializingUrl = baseUrl;
        currentProgress = 0;
        showProgressState();
        updateProgressUi(0);
        updateProgressTip("正在获取活动列表，请稍等");

        // 第一步：获取活动列表
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> call = apiService.getActiveInfo();
        
        call.enqueue(new Callback<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> call, @NonNull Response<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> response) {
                if (!isAdded()) return;
                
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("获取活动列表失败，状态码：" + response.code());
                    }
                    
                    ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>> apiResponse = response.body();
                    if (apiResponse == null || apiResponse.getCode() != 200) {
                        throw new IOException(apiResponse != null ? apiResponse.getMessage() : "活动列表接口返回错误");
                    }
                    
                    List<com.largeevent.management.network.dto.ActiveInfoDTO> activeList = apiResponse.getData();
                    if (activeList == null || activeList.isEmpty()) {
                        throw new IOException("活动列表为空");
                    }
                    
                    updateProgress(30);
                    
                    repository.saveActiveList(baseUrl,
                            com.largeevent.management.data.ActiveInfoMapper.toJsonArrayString(activeList));
                    
                    // 选择第一个活动并保存
                    com.largeevent.management.model.BasicInfo.ActiveModel firstActive =
                            com.largeevent.management.data.ActiveInfoMapper.toActiveModel(activeList.get(0));
                    String eventCode = firstActive.id;  // 使用活动ID作为eventCode
                    if (!TextUtils.isEmpty(eventCode)) {
                        AppPreferences.setLastActiveId(requireContext(), firstActive.id);
                        updateProgress(50);
                        updateProgressTip("正在获取活动基础信息，请稍等");
                        
                        // 第二步：获取活动基础信息
                        fetchBasicInfoWithEventCode(baseUrl, eventCode);
                    } else {
                        throw new IOException("活动ID为空");
                    }
                } catch (IOException | JSONException e) {
                    handleInitializationError(e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                handleInitializationError("网络错误：" + t.getMessage());
            }
        });
    }

    private void fetchBasicInfoWithEventCode(String baseUrl, String eventCode) {
        ApiService apiService = NetworkManager.getInstance().getApiService();
        String eqpId = AppPreferences.ensureDeviceCode(requireContext());
        Call<ResponseBody> call = apiService.getBasicInfo(eventCode, eqpId);
        
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;
                
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("下载失败，状态码：" + response.code());
                    }
                    
                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new IOException("响应体为空");
                    }
                    
                    updateProgress(75);
                    String payload = body.string();
                    
                    JSONObject root = new JSONObject(payload);
                    int code = root.optInt("code", -1);
                    if (code != 200) {
                        throw new IOException(root.optString("message", "基础信息接口返回错误"));
                    }
                    
                    JSONObject data = root.optJSONObject("data");
                    if (data == null) {
                        throw new IOException("基础信息数据为空");
                    }
                    
                    updateProgress(90);
                    repository.saveBaseInfo(baseUrl, data.toString());
                    
                    updateProgress(100);
                    notifyInitializationCompleted();
                } catch (IOException | JSONException e) {
                    handleInitializationError(e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                handleInitializationError("网络错误：" + t.getMessage());
            }
        });
    }

    // 仅获取活动列表（用于定时刷新）
    private void fetchActiveList(String baseUrl) {
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> call = apiService.getActiveInfo();
        
        call.enqueue(new Callback<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> call, @NonNull Response<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> response) {
                if (!isAdded()) return;
                
                try {
                    if (!response.isSuccessful()) {
                        android.util.Log.w("HomeFragment", "自动刷新活动列表失败，状态码：" + response.code());
                        return;
                    }
                    
                    ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>> apiResponse = response.body();
                    if (apiResponse == null || apiResponse.getCode() != 200) {
                        android.util.Log.w("HomeFragment", "自动刷新活动列表失败：" + (apiResponse != null ? apiResponse.getMessage() : "接口返回错误"));
                        return;
                    }
                    
                    List<com.largeevent.management.network.dto.ActiveInfoDTO> activeList = apiResponse.getData();
                    if (activeList == null || activeList.isEmpty()) {
                        android.util.Log.w("HomeFragment", "自动刷新活动列表为空");
                        return;
                    }
                    
                    repository.saveActiveList(baseUrl,
                            com.largeevent.management.data.ActiveInfoMapper.toJsonArrayString(activeList));
                    android.util.Log.d("HomeFragment", "自动刷新活动列表成功");
                } catch (JSONException e) {
                    android.util.Log.w("HomeFragment", "自动刷新活动列表失败：" + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<com.largeevent.management.network.dto.ActiveInfoDTO>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                android.util.Log.w("HomeFragment", "自动刷新活动列表网络错误：" + t.getMessage());
            }
        });
    }

    private void updateProgress(int percent) {
        currentProgress = Math.min(100, Math.max(percent, currentProgress));
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> updateProgressUi(currentProgress));
    }

    private void updateProgressUi(int percent) {
        if (progressBar != null) {
            progressBar.setProgress(percent);
        }
        if (tvProgressPercent != null) {
            tvProgressPercent.setText(percent + "%");
        }
    }

    private void updateProgressTip(String message) {
        if (tvProgressTip != null) {
            tvProgressTip.setText(message);
        }
    }

    private void notifyInitializationCompleted() {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            updateProgressTip("初始化完成");
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onInitializationCompleted(initializingUrl);
            }
            showSuccessState();
            Toast.makeText(requireContext(), "数据下载完成，可进行核验操作", Toast.LENGTH_SHORT).show();

            lastTestSuccess = false;
            lastTestedUrl = "";
            
            // 启动定时自动初始化
            startAutoInitialization();
        });
    }

    private void handleInitializationError(String errorMessage) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            showErrorState("初始化失败");
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleTestButton(boolean enable) {
        if (btnTestConnection == null) {
            return;
        }
        btnTestConnection.setEnabled(enable);
        btnTestConnection.setText(enable ? "测试连接" : "测试中...");
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    private String normalizeUrl(@NonNull String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://" + url;
        }
        return url;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        btnTestConnection = null;
        btnConfirm = null;
        progressBar = null;
        tvProgressPercent = null;
        tvProgressTip = null;
        btnModifyConnection = null;
        btnRetryDownload = null;
        tvErrorTitle = null;
        successLayout = null;
        cardPersonVerify = null;
        cardVehicleVerify = null;
        cardRecord = null;
        errorLayout = null;
        introContainer = null;
        initCard = null;
        progressLayout = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理定时任务
        if (autoInitHandler != null && autoInitRunnable != null) {
            autoInitHandler.removeCallbacks(autoInitRunnable);
        }
        // 注销SharedPreferences监听器
        if (appEventsPrefs != null) {
            appEventsPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        executorService.shutdownNow();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (repository != null) {
            repository.close();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // 监听服务器地址变更
        if ("server_url_updated".equals(key)) {
            String newServerUrl = sharedPreferences.getString(key, "");
            if (!TextUtils.isEmpty(newServerUrl)) {
                // 更新自动初始化的服务器地址
                updateAutoInitializationUrl(newServerUrl);
            }
        }
    }

    // 添加更新自动初始化URL的方法
    private void updateAutoInitializationUrl(String newServerUrl) {
        // 重新启动自动初始化任务，使用新的服务器地址
        if (autoInitHandler != null && autoInitRunnable != null) {
            autoInitHandler.removeCallbacks(autoInitRunnable);
            // 立即执行一次自动初始化以使用新的服务器地址
            performAutoInitializationWithUrl(newServerUrl);
            // 重新启动定时任务
            startAutoInitialization();
        }
    }

    // 添加使用指定URL执行自动初始化的方法
    private void performAutoInitializationWithUrl(String serverUrl) {
        if (!TextUtils.isEmpty(serverUrl)) {
            startInitializationDownload(serverUrl);
        }
    }
}

