package com.largeevent.management.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * 手持机扫码（广播软解）封装，对接厂商扫描服务。
 * 协议参考 SDK「扫描_软解_v1.6」。
 */
public class ScanHelper {

    private static final String TAG = "ScanHelper";

    private static final String ACTION_SCAN_INIT = "com.rfid.SCAN_INIT";
    private static final String ACTION_SCAN = "com.rfid.SCAN_CMD";
    private static final String ACTION_STOP_SCAN = "com.rfid.STOP_SCAN";
    private static final String ACTION_CLOSE_SCAN = "com.rfid.CLOSE_SCAN";
    private static final String ACTION_SET_SCAN_MODE = "com.rfid.SET_SCAN_MODE";
    private static final String ACTION_SCAN_RESULT = "com.rfid.SCAN";

    public interface ScanListener {
        void onScanResult(String barcode);
    }

    private final Context context;
    private ScanListener listener;
    private boolean registered;
    private boolean opened;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent == null || !ACTION_SCAN_RESULT.equals(intent.getAction())) {
                return;
            }
            byte[] data = intent.getByteArrayExtra("data");
            if (data == null || data.length == 0) {
                return;
            }
            String barcode = new String(data).trim();
            if (TextUtils.isEmpty(barcode)) {
                return;
            }
            Log.d(TAG, "scan result: " + barcode);
            if (listener != null) {
                listener.onScanResult(barcode);
            }
        }
    };

    public ScanHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setListener(@Nullable ScanListener listener) {
        this.listener = listener;
    }

    public void open() {
        if (opened) {
            return;
        }
        IntentFilter filter = new IntentFilter(ACTION_SCAN_RESULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
        registered = true;

        Intent init = new Intent(ACTION_SCAN_INIT);
        context.sendBroadcast(init);

        Intent mode = new Intent(ACTION_SET_SCAN_MODE);
        mode.putExtra("mode", 0); // 广播模式
        context.sendBroadcast(mode);
        opened = true;
    }

    public void scan() {
        if (!opened) {
            open();
        }
        Intent intent = new Intent(ACTION_SCAN);
        context.sendBroadcast(intent);
    }

    public void stopScan() {
        Intent intent = new Intent(ACTION_STOP_SCAN);
        context.sendBroadcast(intent);
    }

    public void close() {
        try {
            stopScan();
            Intent close = new Intent(ACTION_CLOSE_SCAN);
            context.sendBroadcast(close);
        } catch (Exception e) {
            Log.w(TAG, "close scan failed", e);
        }
        if (registered) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception e) {
                Log.w(TAG, "unregister scan receiver failed", e);
            }
            registered = false;
        }
        opened = false;
    }
}
