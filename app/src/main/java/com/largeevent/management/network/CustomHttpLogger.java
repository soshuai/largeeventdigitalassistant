package com.largeevent.management.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * 自定义 HTTP 日志拦截器
 * 请求体中的长 Base64 等字段会截断后再打印，避免 Logcat 显示不全
 */
public class CustomHttpLogger implements Interceptor {

    private static final int MAX_LOG_LENGTH = 3000;
    /** 超过该长度的字符串在日志中截断显示 */
    private static final int TRUNCATE_THRESHOLD = 80;
    private static final int LOG_HEAD_LEN = 32;
    private static final int LOG_TAIL_LEN = 16;

    private static final String TAG = "HTTP_LOG";
    private static final Gson GSON = new Gson();

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();

        StringBuilder requestLog = new StringBuilder();
        requestLog.append("\n╔══════════════════════════════════════════════════════════════════════════════════════\n");
        requestLog.append("║ HTTP REQUEST\n");
        requestLog.append("╠══════════════════════════════════════════════════════════════════════════════════════\n");
        requestLog.append("║ URL: ").append(request.url()).append("\n");
        requestLog.append("║ Method: ").append(request.method()).append("\n");

        if (request.headers().size() > 0) {
            requestLog.append("║ Headers:\n");
            for (int i = 0; i < request.headers().size(); i++) {
                requestLog.append("║   ")
                        .append(request.headers().name(i))
                        .append(": ")
                        .append(request.headers().value(i))
                        .append("\n");
            }
        }

        if (request.body() != null) {
            try {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                String requestBody = buffer.readUtf8();
                requestLog.append("║ Body:\n");
                requestLog.append("║ ").append(sanitizeBodyForLog(requestBody)).append("\n");
            } catch (Exception e) {
                requestLog.append("║ Body: [无法读取]\n");
            }
        }

        requestLog.append("╚══════════════════════════════════════════════════════════════════════════════════════");
        printLongLog(requestLog.toString());

        Response response = chain.proceed(request);

        StringBuilder responseLog = new StringBuilder();
        responseLog.append("\n╔══════════════════════════════════════════════════════════════════════════════════════\n");
        responseLog.append("║ HTTP RESPONSE\n");
        responseLog.append("╠══════════════════════════════════════════════════════════════════════════════════════\n");
        responseLog.append("║ URL: ").append(response.request().url()).append("\n");
        responseLog.append("║ Status: ").append(response.code()).append(" ").append(response.message()).append("\n");

        if (response.headers().size() > 0) {
            responseLog.append("║ Headers:\n");
            for (int i = 0; i < response.headers().size(); i++) {
                responseLog.append("║   ")
                        .append(response.headers().name(i))
                        .append(": ")
                        .append(response.headers().value(i))
                        .append("\n");
            }
        }

        if (response.body() != null) {
            try {
                ResponseBody responseBody = response.body();
                String bodyString = responseBody.string();
                responseLog.append("║ Body:\n");
                responseLog.append("║ ").append(sanitizeBodyForLog(bodyString)).append("\n");
                response = response.newBuilder()
                        .body(ResponseBody.create(responseBody.contentType(), bodyString))
                        .build();
            } catch (Exception e) {
                responseLog.append("║ Body: [无法读取]\n");
            }
        }

        responseLog.append("╚══════════════════════════════════════════════════════════════════════════════════════");
        printLongLog(responseLog.toString());

        return response;
    }

    /**
     * 将 JSON 请求/响应体中的超长字符串（如 Base64）截断后再输出到日志
     */
    @Nullable
    static String sanitizeBodyForLog(@Nullable String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        if (body.length() <= TRUNCATE_THRESHOLD) {
            return body;
        }
        try {
            JsonElement root = JsonParser.parseString(body);
            truncateLongStringsInJson(root);
            return GSON.toJson(root);
        } catch (Exception ignored) {
            return truncatePlainText(body);
        }
    }

    private static void truncateLongStringsInJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (String key : obj.keySet()) {
                JsonElement child = obj.get(key);
                if (child != null && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    String value = child.getAsString();
                    if (value.length() > TRUNCATE_THRESHOLD) {
                        obj.addProperty(key, truncateForLog(value));
                    }
                } else {
                    truncateLongStringsInJson(child);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement item = array.get(i);
                if (item != null && item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                    String value = item.getAsString();
                    if (value.length() > TRUNCATE_THRESHOLD) {
                        array.set(i, new JsonPrimitive(truncateForLog(value)));
                    }
                } else {
                    truncateLongStringsInJson(item);
                }
            }
        }
    }

    private static String truncateForLog(String value) {
        if (value.length() <= TRUNCATE_THRESHOLD) {
            return value;
        }
        int omitted = value.length() - LOG_HEAD_LEN - LOG_TAIL_LEN;
        if (omitted <= 0) {
            return value.substring(0, TRUNCATE_THRESHOLD) + "...(len=" + value.length() + ")";
        }
        return value.substring(0, LOG_HEAD_LEN)
                + "...(省略" + omitted + "字)..."
                + value.substring(value.length() - LOG_TAIL_LEN)
                + " [len=" + value.length() + "]";
    }

    /** 非 JSON 体：按行截断超长内容 */
    private static String truncatePlainText(String body) {
        if (body.length() <= TRUNCATE_THRESHOLD) {
            return body;
        }
        return truncateForLog(body);
    }

    private void printLongLog(String log) {
        if (log.length() <= MAX_LOG_LENGTH) {
            Log.d(TAG, log);
            return;
        }
        int start = 0;
        while (start < log.length()) {
            int end = Math.min(start + MAX_LOG_LENGTH, log.length());
            Log.d(TAG, log.substring(start, end));
            start = end;
        }
    }
}
