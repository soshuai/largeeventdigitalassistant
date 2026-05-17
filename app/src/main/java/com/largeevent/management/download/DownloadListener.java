package com.largeevent.management.download;

/**
 * 下载监听器接口
 */
public interface DownloadListener {
    /**
     * 下载进度回调
     * @param progress 进度百分比
     */
    void onProgress(int progress);

    /**
     * 下载完成回调
     * @param filePath 文件保存路径
     */
    void onComplete(String filePath);

    /**
     * 下载出错回调
     * @param error 错误信息
     */
    void onError(String error);
}