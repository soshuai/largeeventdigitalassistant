package com.largeevent.management.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.largeevent.management.network.dto.BindCertDTO;
import com.largeevent.management.network.dto.FaceMatchParamDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * 自定义 HTTP 日志拦截器
 * 一次性打印完整的请求和响应内容，方便复制
 */
public class CustomHttpLogger implements Interceptor {

    // Logcat 的单行最大长度限制
    private static final int MAX_LOG_LENGTH = 3000;

    private static final String TAG = "HTTP_LOG";

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();

        // 构建请求日志
        StringBuilder requestLog = new StringBuilder();
        requestLog.append("\n╔══════════════════════════════════════════════════════════════════════════════════════\n");
        requestLog.append("║ HTTP REQUEST\n");
        requestLog.append("╠══════════════════════════════════════════════════════════════════════════════════════\n");
        requestLog.append("║ URL: ").append(request.url()).append("\n");
        requestLog.append("║ Method: ").append(request.method()).append("\n");

        // 请求头
        if (request.headers().size() > 0) {
            requestLog.append("║ Headers:\n");
            for (int i = 0; i < request.headers().size(); i++) {
                requestLog.append("║   ").append(request.headers().name(i)).append(": ").append(request.headers().value(i)).append("\n");
            }
        }

        // 请求体
        if (request.body() != null) {
            try {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                String requestBody = buffer.readUtf8();
                requestLog.append("║ Body:\n");
                // 如果是人脸比对接口，隐藏 Base64 图片数据
                if (request.url().toString().contains("baidu/faceMatch")) {
                    String sanitizedBody = sanitizeFaceMatchBody(requestBody);
                    requestLog.append("║ [Face Match Request - Base64 Images Hidden]\n");
                    requestLog.append("║ ").append(sanitizedBody).append("\n");
                } else if (request.url().toString().contains("android/api/bandCert")) {
                    String sanitizedBody = sanitizeBindCertBody(requestBody);
                    requestLog.append("║ [Face Match Request - Base64 Images Hidden]\n");
                    requestLog.append("║ ").append(sanitizedBody).append("\n");
                } else {
                    requestLog.append("║ ").append(requestBody).append("\n");
                }
            } catch (Exception e) {
                requestLog.append("║ Body: [无法读取]\n");
            }
        }

        requestLog.append("╚══════════════════════════════════════════════════════════════════════════════════════");

        // 分段打印，避免内容被截断
        printLongLog(requestLog.toString());

        // 执行请求
        Response response = chain.proceed(request);

        // 构建响应日志
        StringBuilder responseLog = new StringBuilder();
        responseLog.append("\n╔══════════════════════════════════════════════════════════════════════════════════════\n");
        responseLog.append("║ HTTP RESPONSE\n");
        responseLog.append("╠══════════════════════════════════════════════════════════════════════════════════════\n");
        responseLog.append("║ URL: ").append(response.request().url()).append("\n");
        responseLog.append("║ Status: ").append(response.code()).append(" ").append(response.message()).append("\n");

        // 响应头
        if (response.headers().size() > 0) {
            responseLog.append("║ Headers:\n");
            for (int i = 0; i < response.headers().size(); i++) {
                responseLog.append("║   ").append(response.headers().name(i)).append(": ").append(response.headers().value(i)).append("\n");
            }
        }

        // 响应体
        String responseBodyString = "";
        if (response.body() != null) {
            try {
                ResponseBody responseBody = response.body();
                String bodyString = responseBody.string();
                responseBodyString = bodyString;

                responseLog.append("║ Body:\n");
                responseLog.append("║ ").append(bodyString).append("\n");

                // 重新创建响应体，因为已经读取过了
                response = response.newBuilder().body(ResponseBody.create(responseBody.contentType(), bodyString)).build();
            } catch (Exception e) {
                responseLog.append("║ Body: [无法读取]\n");
            }
        }

        responseLog.append("╚══════════════════════════════════════════════════════════════════════════════════════");

        // 分段打印，避免内容被截断
        printLongLog(responseLog.toString());

        return response;
    }

    /**
     * 分段打印长日志，避免被截断
     */
    private void printLongLog(String log) {
        if (log.length() <= MAX_LOG_LENGTH) {
            System.out.println(log);
            return;
        }

        // 分段打印
        int start = 0;
        while (start < log.length()) {
            int end = Math.min(start + MAX_LOG_LENGTH, log.length());
            System.out.println(log.substring(start, end));
            start = end;
        }
    }

    /**
     * 隐藏人脸比对接口中的 Base64 图片数据
     */
    private String sanitizeFaceMatchBody(String body) {
        try {
            // 将 JSON 字符串转成 MutableList<FaceMatchParamDTO>
            Gson gson = new Gson();
            Type listType = new TypeToken<List<FaceMatchParamDTO>>() {
            }.getType();
            List<FaceMatchParamDTO> faceMatchParams = gson.fromJson(body, listType);

            if (faceMatchParams != null) {
                // 隐藏每个参数中的 Base64 图片数据
                for (FaceMatchParamDTO param : faceMatchParams) {
                    if (param != null && param.getImage() != null && param.getImage().length() > 50) {
                        param.setImage("[Base64 Data Hidden - Length: " + param.getImage().length() + "]");
                    }
                }
                // 转回 JSON 字符串
                return gson.toJson(faceMatchParams);
            }

            return body;
        } catch (Exception e) {
            return "Base64 Data Hidden";
        }
    }


    private String sanitizeBindCertBody(String body) {
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<BindCertDTO>() {
            }.getType();
            BindCertDTO bindCertDTO = gson.fromJson(body, listType);

            if (bindCertDTO != null) {
                if (bindCertDTO != null && bindCertDTO.base64Img != null && bindCertDTO.base64Img.length() > 50) {
                    bindCertDTO.base64Img = "[Base64 Data Hidden - Length: " + bindCertDTO.base64Img.length() + "]";
                }
                // 转回 JSON 字符串
                return gson.toJson(bindCertDTO);
            }

            return body;
        } catch (Exception e) {
            return "Base64 Data Hidden";
        }
    }
}
