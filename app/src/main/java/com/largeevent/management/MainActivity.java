package com.largeevent.management;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.handheld.uhfr.UHFRManager;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.nfc.NfcCallback;
import com.largeevent.management.nfc.NfcManager;
import com.largeevent.management.ui.HomeFragment;
import com.largeevent.management.ui.PersonVerifyFragment;
import com.largeevent.management.ui.SettingsFragment;
import com.largeevent.management.ui.CarVerifyFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String TAG_HOME = "tag_home";
    private static final String TAG_PERSON = "tag_person";
    private static final String TAG_VEHICLE = "tag_vehicle";
    private static final String TAG_SETTINGS = "tag_settings";

    private BottomNavigationView bottomNavigationView;
    private NfcManager nfcManager;
    private View statusBarPlaceholder;
    public UHFRManager mUhfrManager;//uhf

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查软件使用时间限制（1周）
//        if (!checkAppTimeLimit()) {
//            showExpiredDialog();
//            return;
//        }
        
        setContentView(R.layout.activity_main);

        // 设置状态栏占位视图高度
        statusBarPlaceholder = findViewById(R.id.status_bar_placeholder);
        setStatusBarPlaceholderHeight();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        nfcManager = new NfcManager(this);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // 检查是否需要初始化检查
            if (itemId == R.id.nav_person_verify || itemId == R.id.nav_vehicle_verify) {
                if (!isAppInitialized()) {
                    Toast.makeText(this, "请先到首页完成初始化", Toast.LENGTH_LONG).show();
                    // 返回 false 表示不切换菜单项
                    return false;
                }
            }
            
            switch (itemId) {
                case R.id.nav_home:
                    showFragment(TAG_HOME);
                    return true;
                case R.id.nav_person_verify:
                    showFragment(TAG_PERSON);
                    return true;
                case R.id.nav_vehicle_verify:
                    showFragment(TAG_VEHICLE);
                    return true;
                case R.id.nav_settings:
                    showFragment(TAG_SETTINGS);
                    return true;
                default:
                    return false;
            }
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
        
        // 处理启动 Intent（可能是 NFC Intent）
        handleNfcIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcManager != null) {
            nfcManager.enableForegroundDispatch();
        }
        // 确保当前 Fragment 的 NFC 回调已设置（但不处理 Intent）
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            configureNfcCallback(currentFragment);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcManager != null) {
            nfcManager.disableForegroundDispatch();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent != null ? intent.getAction() : "null";
        boolean hasNfcTag = intent != null && intent.hasExtra(android.nfc.NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "onNewIntent called, intent action: " + action + ", has NFC tag: " + hasNfcTag);
        
        // 更新 Activity 的 Intent，这样 getIntent() 才能获取到新的 Intent
        setIntent(intent);
        // 处理 NFC Intent
        handleNfcIntent(intent);
    }
    
    /**
     * 处理 NFC Intent
     */
    private void handleNfcIntent(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "handleNfcIntent: intent is null");
            return;
        }
        
        String action = intent.getAction();
        boolean hasNfcTag = intent.hasExtra(android.nfc.NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "handleNfcIntent: action=" + action + ", has NFC tag: " + hasNfcTag);
        
        // 如果有 NFC Tag 数据，但 action 为 null，尝试直接处理
        if (hasNfcTag) {
            Log.d(TAG, "NFC Tag data detected, processing...");
            // 切换到人证页面
            bottomNavigationView.setSelectedItemId(R.id.nav_person_verify);
            
            // 确保当前 Fragment 的 NFC 回调已设置
            configureCurrentFragmentNfcCallback();
            
            // 处理 NFC Intent
            if (nfcManager != null) {
                nfcManager.handleIntent(intent);
            }
            return;
        }
        
        // 如果 action 为 null，不处理
        if (action == null) {
            Log.w(TAG, "handleNfcIntent: action is null and no NFC tag, skipping NFC processing");
            return;
        }
        
        // 如果是 NFC Intent，切换到人证页面
        if (android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.d(TAG, "NFC Intent detected, switching to person verify page");
            // 切换到人证页面
            bottomNavigationView.setSelectedItemId(R.id.nav_person_verify);
            
            // 确保当前 Fragment 的 NFC 回调已设置
            configureCurrentFragmentNfcCallback();
            
            // 处理 NFC Intent
            if (nfcManager != null) {
                nfcManager.handleIntent(intent);
            }
        } else {
            Log.d(TAG, "Not an NFC Intent, action: " + action);
        }
    }

    private void showFragment(@NonNull String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = createFragmentByTag(tag);
        }
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
        configureNfcCallback(fragment);
    }

    private void configureNfcCallback(Fragment fragment) {
        if (nfcManager == null) {
            return;
        }
        if (fragment instanceof NfcCallback) {
            nfcManager.setNfcCallback((NfcCallback) fragment);
        } else {
            nfcManager.setNfcCallback(null);
        }
    }

    /**
     * 配置当前显示的 Fragment 的 NFC 回调
     */
    private void configureCurrentFragmentNfcCallback() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            configureNfcCallback(currentFragment);
        }
    }

    private Fragment createFragmentByTag(@NonNull String tag) {
        switch (tag) {
            case TAG_PERSON:
                return new PersonVerifyFragment();
            case TAG_VEHICLE:
                return new CarVerifyFragment();
            case TAG_SETTINGS:
                return new SettingsFragment();
            case TAG_HOME:
            default:
                return new HomeFragment();
        }
    }

    public void onInitializationCompleted(@NonNull String serverUrl) {
        Toast.makeText(this, "初始化完成，服务器：" + serverUrl, Toast.LENGTH_SHORT).show();
    }

    public void navigateToTab(@IdRes int menuId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(menuId);
        }
    }

    public void showNfcStatusHint() {
        if (nfcManager != null) {
            nfcManager.showNfcStatus();
        }
    }

    // 添加设置状态栏占位视图高度的方法
    private void setStatusBarPlaceholderHeight() {
        if (statusBarPlaceholder != null) {
            int statusBarHeight = getStatusBarHeight();
            ViewGroup.LayoutParams layoutParams = statusBarPlaceholder.getLayoutParams();
            layoutParams.height = statusBarHeight;
            statusBarPlaceholder.setLayoutParams(layoutParams);
        }
    }

    // 获取状态栏高度
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    
    /**
     * 检查应用是否已初始化
     */
    private boolean isAppInitialized() {
        // 检查是否有 activeId
        String activeId = AppPreferences.getLastActiveId(this);
        if (!TextUtils.isEmpty(activeId)) {
            return true;
        }
        
        // 检查数据库中是否有数据
        InitializationRepository repository = null;
        try {
            repository = new InitializationRepository(this);
            if (repository.hasData()) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check initialization status", e);
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
        
        return false;
    }
    
    /**
     * 检查软件使用时间限制（1周）
     * @return true=未过期，false=已过期
     */
    private boolean checkAppTimeLimit() {
        SharedPreferences prefs = getSharedPreferences("app_config", MODE_PRIVATE);
        long firstInstallTime = prefs.getLong("first_install_time", 0L);
        
        // 如果是首次运行，记录安装时间
        if (firstInstallTime == 0L) {
            firstInstallTime = System.currentTimeMillis();
            prefs.edit().putLong("first_install_time", firstInstallTime).apply();
            Log.d(TAG, "首次运行，记录安装时间: " + new java.util.Date(firstInstallTime));
        }
        
        // 计算已经过去的天数
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - firstInstallTime;
        long daysPassed = timeDiff / (1000L * 60 * 60 * 24);
        // 如果超过 5 天，返回 false
        return daysPassed < 7;
    }
    
    /**
     * 显示软件过期对话框
     */
    private void showExpiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("软件使用期限已到")
                .setMessage("此应用的使用期限已经过期（1周），无法继续使用。")
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {
                    // 关闭应用
                    finish();
                    System.exit(0);
                })
                .show();
    }
}

