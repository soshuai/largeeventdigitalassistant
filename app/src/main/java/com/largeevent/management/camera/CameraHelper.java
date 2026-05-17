package com.largeevent.management.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 拍照工具类
 */
public class CameraHelper {
    public static final int REQUEST_CODE_TAKE_PHOTO = 1001;
    public static final int REQUEST_CODE_SELECT_IMAGE = 1002;
    private final Activity activity;
    private Fragment fragment;
    private String currentPhotoPath;
    private CameraCallback callback;

    public CameraHelper(Activity activity) {
        this.activity = activity;
    }

    public CameraHelper(Fragment fragment) {
        this.activity = fragment.requireActivity();
        this.fragment = fragment;
    }

    /**
     * 设置拍照回调
     */
    public void setCameraCallback(CameraCallback callback) {
        this.callback = callback;
    }

    /**
     * 拍照
     */
    public void takePhoto() {
        takePhoto(false);
    }

    /**
     * 拍照（可选前置摄像头）
     */
    public void takePhoto(boolean useFrontCamera) {
        if (checkCameraPermission()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Toast.makeText(activity, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(activity,
                            activity.getPackageName() + ".fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    if (useFrontCamera) {
                        takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                        takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                        takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
                        takePictureIntent.putExtra("android.intent.extras.LENS_FACING_BACK", 0);
                    }
                    if (fragment != null) {
                        fragment.startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO);
                    } else {
                        activity.startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO);
                    }
                }
            }
        }
    }

    /**
     * 从相册选择图片
     */
    public void selectFromAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (fragment != null) {
            fragment.startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        } else {
            activity.startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    /**
     * 检查相机权限
     */
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_TAKE_PHOTO);
            return false;
        }
        return true;
    }

    /**
     * 创建图片文件
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * 获取当前照片路径
     */
    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }

    /**
     * 处理拍照结果
     */
    public void handleTakePhotoResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            if (callback != null) {
                callback.onPhotoTaken(currentPhotoPath);
            }
        }
    }

    /**
     * 处理选择图片结果
     */
    public void handleSelectImageResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (callback != null) {
                callback.onImageSelected(selectedImage);
            }
        }
    }
}