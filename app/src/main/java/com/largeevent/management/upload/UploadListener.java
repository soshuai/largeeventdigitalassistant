package com.largeevent.management.upload;

/**
 * 文件上传监听器接口
 */
public interface UploadListener {
    /**
     * 上传进度回调
     * @param progress 进度百分比
     */
    void onProgress(int progress);

    /**
     * 上传成功回调
     * @param response 响应数据
     */
    void onSuccess(String response);

    /**
     * 上传失败回调
     * @param error 错误信息
     */
    void onError(String error);
}