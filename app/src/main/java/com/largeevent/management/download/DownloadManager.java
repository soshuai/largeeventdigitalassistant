package com.largeevent.management.download;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 文件下载管理类
 */
public class DownloadManager {
    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1001;
    private Context context;
    private NotificationManager notificationManager;
    private DownloadListener listener;

    public DownloadManager(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    /**
     * 设置下载监听器
     */
    public void setDownloadListener(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 创建通知渠道（Android 8.0及以上）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "文件下载";
            String description = "用于显示文件下载进度";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 下载文件
     * @param fileUrl 文件URL
     * @param fileName 保存的文件名
     */
    public void downloadFile(String fileUrl, String fileName) {
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    int fileSize = connection.getContentLength();
                    InputStream inputStream = connection.getInputStream();

                    File file = new File(context.getExternalFilesDir(null), fileName);
                    FileOutputStream outputStream = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytesRead = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        // 计算进度
                        int progress = (int) ((totalBytesRead * 100L) / fileSize);

                        // 更新进度通知
                        updateNotification(progress, fileName);

                        // 回调进度
                        if (listener != null) {
                            listener.onProgress(progress);
                        }
                    }

                    outputStream.close();
                    inputStream.close();

                    // 下载完成通知
                    completeNotification(fileName);

                    // 回调完成
                    if (listener != null) {
                        listener.onComplete(file.getAbsolutePath());
                    }
                } else {
                    if (listener != null) {
                        listener.onError("下载失败，响应码：" + responseCode);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) {
                    listener.onError("下载异常：" + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 更新下载进度通知
     */
    private void updateNotification(int progress, String fileName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("正在下载")
                .setContentText(fileName + " - " + progress + "%")
                .setProgress(100, progress, false)
                .setOngoing(true);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * 下载完成通知
     */
    private void completeNotification(String fileName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("下载完成")
                .setContentText(fileName)
                .setProgress(0, 0, false)
                .setOngoing(false);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}