package com.largeevent.management.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.scarx.util.SerialPort;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;
import com.zkteco.android.biometric.module.idcard.meta.IDPRPCardInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 身份证 RFID 读卡封装，对接 ZK IDRDemo_Com13_Rfid_SDK_v1.15。
 */
public class IdCardReaderHelper {

    private static final String TAG = "IdCardReaderHelper";
    /** 后端身份证类型编码（与 getActiveUserByIdNumber 示例一致） */
    public static final String DEFAULT_ID_TYPE = "169";

    public static class IdCardResult {
        public final String name;
        public final String idNumber;
        public final String idType;

        public IdCardResult(String name, String idNumber, String idType) {
            this.name = name;
            this.idNumber = idNumber;
            this.idType = idType;
        }
    }

    public interface Callback {
        void onSuccess(IdCardResult result);

        void onError(String message);

        void onProgress(String message);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private IDCardReader idCardReader;
    private final AtomicBoolean opened = new AtomicBoolean(false);
    private final AtomicBoolean reading = new AtomicBoolean(false);
    private Thread readThread;

    public IdCardReaderHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized boolean open() {
        if (opened.get()) {
            return true;
        }
        try {
            boolean isF = new SerialPort().DCENon();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LogHelper.setLevel(Log.ASSERT);
            Map<String, Object> params = new HashMap<>(2);
            params.put(ParameterHelper.PARAM_SERIAL_SERIALNAME, isF ? "/dev/ttyS1" : "/dev/ttyMT1");
            params.put(ParameterHelper.PARAM_SERIAL_BAUDRATE, 115200);
            idCardReader = IDCardReaderFactory.createIDCardReader(
                    context, TransportType.SERIALPORT, params);
            idCardReader.open(0);
            opened.set(true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "open ID card reader failed", e);
            opened.set(false);
            safePowerOff();
            return false;
        }
    }

    public synchronized void close() {
        stopReading();
        if (idCardReader != null) {
            try {
                idCardReader.close(0);
            } catch (Exception e) {
                Log.w(TAG, "close reader failed", e);
            }
            idCardReader = null;
        }
        opened.set(false);
        safePowerOff();
    }

    public void startReadOnce(Callback callback) {
        if (!open()) {
            notifyError(callback, "身份证模块打开失败");
            return;
        }
        if (reading.get()) {
            notifyProgress(callback, "正在读卡，请稍候");
            return;
        }
        reading.set(true);
        notifyProgress(callback, "请将身份证靠近读卡区");
        readThread = new Thread(() -> {
            long deadline = System.currentTimeMillis() + 15000L;
            try {
                while (reading.get() && System.currentTimeMillis() < deadline) {
                    if (idCardReader == null) {
                        break;
                    }
                    try {
                        if (!idCardReader.findCard(0)) {
                            Thread.sleep(200);
                            continue;
                        }
                        if (!idCardReader.selectCard(0)) {
                            Thread.sleep(200);
                            continue;
                        }
                        notifyProgress(callback, "发现身份证，正在读取…");
                        int readCardEx = idCardReader.readCardEx(0, 0);
                        if (readCardEx == 1 || readCardEx == 3) {
                            IDCardInfo info = idCardReader.getLastIDCardInfo();
                            if (info != null && !TextUtils.isEmpty(info.getId())) {
                                IdCardResult result = new IdCardResult(
                                        safe(info.getName()),
                                        info.getId().trim(),
                                        DEFAULT_ID_TYPE);
                                reading.set(false);
                                notifySuccess(callback, result);
                                return;
                            }
                        } else if (readCardEx == 2 || readCardEx == 4) {
                            IDPRPCardInfo info = idCardReader.getLastPRPIDCardInfo();
                            if (info != null && !TextUtils.isEmpty(info.getId())) {
                                String name = safe(info.getEnName());
                                IdCardResult result = new IdCardResult(
                                        name,
                                        info.getId().trim(),
                                        DEFAULT_ID_TYPE);
                                reading.set(false);
                                notifySuccess(callback, result);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "read loop error: " + e.getMessage());
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (reading.get()) {
                    reading.set(false);
                    notifyError(callback, "读卡超时，请重试");
                }
            } finally {
                reading.set(false);
            }
        }, "IdCardReadThread");
        readThread.start();
    }

    public void stopReading() {
        reading.set(false);
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
    }

    private void safePowerOff() {
        try {
            new SerialPort().DCENoff();
        } catch (Exception e) {
            Log.w(TAG, "power off failed", e);
        }
    }

    private static String safe(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    private void notifySuccess(Callback callback, IdCardResult result) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void notifyError(Callback callback, String message) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onError(message));
    }

    private void notifyProgress(Callback callback, String message) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onProgress(message));
    }
}
