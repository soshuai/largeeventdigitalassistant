package com.largeevent.management.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * 网络请求管理类
 */
public class NetworkManager {
    private static NetworkManager instance;
    private Retrofit retrofit;
    private String currentBaseUrl = "http://localhost/";  // 默认占位值，实际使用时必须设置
    private Gson gson;

    private NetworkManager() {
        initRetrofit(currentBaseUrl);
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    private void initRetrofit(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new CustomHttpLogger())
                .build();

        // 与后端保持一致：JSON 使用 camelCase（id、activeName、code 等）
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .disableHtmlEscaping()
                .setLenient()
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * 获取API服务实例
     */
    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }
    
    /**
     * 获取统一 API 服务实例
     */
    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        if (baseUrl.equals(currentBaseUrl)) {
            return;
        }
        currentBaseUrl = baseUrl;
        initRetrofit(currentBaseUrl);
    }

    public String getCurrentBaseUrl() {
        return currentBaseUrl;
    }

    public Gson getGson() {
        return gson;
    }
}