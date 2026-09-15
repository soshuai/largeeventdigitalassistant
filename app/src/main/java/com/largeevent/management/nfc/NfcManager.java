package com.largeevent.management.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.largeevent.management.R;

/**
 * NFC管理类
 */
public class NfcManager implements NfcAdapter.ReaderCallback {
    private static final String TAG = "NfcManager";
    private Activity activity;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private NfcCallback callback;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    public NfcManager(Activity activity) {
        this.activity = activity;
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        
        // 创建PendingIntent
        Intent intent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }
        
        // 设置IntentFilter
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[] { ndef };
        
        // 设置技术支持列表
        techListsArray = new String[][] {
                new String[] { NfcA.class.getName() },
                new String[] { NfcB.class.getName() },
                new String[] { NfcF.class.getName() },
                new String[] { NfcV.class.getName() },
                new String[] { IsoDep.class.getName() },
                new String[] { MifareClassic.class.getName() },
                new String[] { MifareUltralight.class.getName() },
                new String[] { Ndef.class.getName() },
                new String[] { NdefFormatable.class.getName() }
        };
    }

    /**
     * 设置NFC回调
     */
    public void setNfcCallback(NfcCallback callback) {
        this.callback = callback;
        Log.d(TAG, "setNfcCallback: " + (callback != null ? callback.getClass().getSimpleName() : "null"));
    }

    /**
     * 检查NFC功能是否可用
     */
    public boolean isNfcAvailable() {
        return nfcAdapter != null;
    }

    /**
     * 检查NFC是否已启用
     */
    public boolean isNfcEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    /**
     * 启用NFC前台调度
     */
    public void enableForegroundDispatch() {
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            // 使用 Reader Mode 代替 Foreground Dispatch，更可靠
            int flags = NfcAdapter.FLAG_READER_NFC_A |
                       NfcAdapter.FLAG_READER_NFC_B |
                       NfcAdapter.FLAG_READER_NFC_F |
                       NfcAdapter.FLAG_READER_NFC_V |
                       NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;
            
            Bundle options = new Bundle();
            // 设置较短的轮询间隔，提高响应速度
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
            
            nfcAdapter.enableReaderMode(activity, this, flags, options);
            Log.d(TAG, "NFC Reader Mode enabled");
        }
    }

    /**
     * 禁用NFC前台调度
     */
    public void disableForegroundDispatch() {
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(activity);
            Log.d(TAG, "NFC Reader Mode disabled");
        }
    }

    /**
     * Reader Mode 回调，当检测到 NFC 标签时触发
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        if (tag == null) {
            Log.w(TAG, "onTagDiscovered: tag is null");
            return;
        }
        
        byte[] tagIdBytes = tag.getId();
        // 记录原始字节
        StringBuilder originalHex = new StringBuilder();
        for (byte b : tagIdBytes) {
            originalHex.append(String.format("%02X", b));
        }
        Log.d(TAG, "Tag ID original (forward): " + originalHex.toString());
        
        String tagId = bytesToHex(tagIdBytes);
        String[] techList = tag.getTechList();
        
        Log.d(TAG, "Tag discovered via Reader Mode: ID=" + tagId
                + ", callback=" + (callback != null ? callback.getClass().getSimpleName() : "null"));
        Log.i("HwTrigger", "NFC_READER_MODE tagId=" + tagId
                + " callback=" + (callback != null ? callback.getClass().getSimpleName() : "null")
                + " → 人证页靠这条链路自动读卡；车证页收到也只会更新 chipId，不会启动 UHF");
        
        if (callback != null) {
            // 切换到主线程调用回调
            activity.runOnUiThread(() -> callback.onTagDetected(tagId, techList));
        } else {
            Log.w(TAG, "NFC tag detected but callback is null!");
            activity.runOnUiThread(() -> 
                Toast.makeText(activity, "NFC检测到标签，但回调未设置", Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * 处理NFC意图
     */
    public void handleIntent(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "handleIntent: intent is null!");
            return;
        }
        
        String action = intent.getAction();
        boolean hasTag = intent.hasExtra(NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "handleIntent: action=" + action + ", has NFC tag: " + hasTag);
        
        // 尝试获取 Tag，无论 action 是否为 null
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String tagId = bytesToHex(tag.getId());
            String[] techList = tag.getTechList();
            
            Log.d(TAG, "Tag detected: ID=" + tagId + ", callback=" + (callback != null ? "set" : "null"));
            
            if (callback != null) {
                callback.onTagDetected(tagId, techList);
            } else {
                Log.w(TAG, "NFC tag detected but callback is null!");
                Toast.makeText(activity, "NFC检测到标签，但回调未设置", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        if (action == null) {
            Log.w(TAG, "handleIntent: action is null and no NFC tag found");
            return;
        }
        
        // 检查是否是 NFC action
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.w(TAG, "NFC action detected but no tag data in intent");
        } else {
            Log.d(TAG, "Not an NFC action: " + action);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串（反转字节序）
     * NFC 芯片通常需要反转字节序才能得到正确的序列号
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        // 反转字节序读取
        for (int i = bytes.length - 1; i >= 0; i--) {
            sb.append(String.format("%02X", bytes[i]));
        }
        
        String result = sb.toString();
        Log.d(TAG, "Tag ID bytes length: " + bytes.length + ", reversed hex: " + result);
        return result;
    }

    /**
     * 显示NFC状态提示
     */
    public void showNfcStatus() {
        if (!isNfcAvailable()) {
            Toast.makeText(activity, R.string.nfc_not_supported, Toast.LENGTH_SHORT).show();
        } else if (!isNfcEnabled()) {
            Toast.makeText(activity, R.string.nfc_disabled, Toast.LENGTH_SHORT).show();
        }
    }
}