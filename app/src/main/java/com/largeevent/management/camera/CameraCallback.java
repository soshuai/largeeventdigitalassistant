package com.largeevent.management.camera;

import android.net.Uri;

/**
 * 拍照回调接口
 */
public interface CameraCallback {
    /**
     * 拍照完成回调
     * @param photoPath 照片路径
     */
    void onPhotoTaken(String photoPath);

    /**
     * 选择图片回调
     * @param imageUri 图片URI
     */
    void onImageSelected(Uri imageUri);
}