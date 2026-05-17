package com.largeevent.management.upload;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.largeevent.management.network.CustomHttpLogger;

/**
 * 文件上传管理类
 */
public class UploadManager {
    private Context context;
    private UploadListener listener;
    private OkHttpClient okHttpClient;

    public UploadManager(Context context) {
        this.context = context;
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new CustomHttpLogger())
                .build();
    }

    /**
     * 设置上传监听器
     */
    public void setUploadListener(UploadListener listener) {
        this.listener = listener;
    }

    /**
     * 上传文件
     * @param uploadUrl 上传地址
     * @param filePath 文件路径
     * @param fieldName 表单字段名
     */
    public void uploadFile(String uploadUrl, String filePath, String fieldName) {
        File file = new File(filePath);
        if (!file.exists()) {
            if (listener != null) {
                listener.onError("文件不存在");
            }
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(fieldName, file.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .build();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    if (listener != null) {
                        listener.onSuccess(responseBody);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("上传失败，响应码：" + response.code());
                    }
                }
            }
        });
    }

    /**
     * 上传带进度的文件
     * @param uploadUrl 上传地址
     * @param filePath 文件路径
     * @param fieldName 表单字段名
     */
    public void uploadFileWithProgress(String uploadUrl, String filePath, String fieldName) {
        File file = new File(filePath);
        if (!file.exists()) {
            if (listener != null) {
                listener.onError("文件不存在");
            }
            return;
        }

        // 创建带进度监听的RequestBody
        ProgressRequestBody requestBody = new ProgressRequestBody(
                RequestBody.create(MediaType.parse("application/octet-stream"), file),
                new ProgressRequestBody.UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesWritten, long contentLength) {
                        int progress = (int) ((bytesWritten * 100) / contentLength);
                        if (listener != null) {
                            listener.onProgress(progress);
                        }
                    }
                });

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(fieldName, file.getName(), requestBody)
                        .build())
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    if (listener != null) {
                        listener.onSuccess(responseBody);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("上传失败，响应码：" + response.code());
                    }
                }
            }
        });
    }
}