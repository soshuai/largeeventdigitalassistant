package com.largeevent.management.network;

/**
 * 网络请求回调接口
 */
public interface NetworkCallback<T> {
    /**
     * 请求成功回调
     * @param data 返回的数据
     */
    void onSuccess(T data);

    /**
     * 请求失败回调
     * @param errorCode 错误码
     * @param errorMsg 错误信息
     */
    void onFailure(int errorCode, String errorMsg);
}