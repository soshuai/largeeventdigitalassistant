package com.largeevent.management;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.largeevent.management.camera.CameraCallback;
import com.largeevent.management.camera.CameraHelper;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.BindCertDTO;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 人证绑定页面
 */
public class PersonCertBindActivity extends AppCompatActivity {

    public static final String EXTRA_CHIP_ID = "extra_chip_id";
    public static final String EXTRA_CERT_NUMBER = "extra_cert_number";
    
    private static final String TAG = "PersonCertBindActivity";

    private EditText etPersonName;
    private EditText etIdNumber;
    private TextView btnTakePhoto;
    private ImageView ivPhotoPreview;
    private Button btnCancel;
    private Button btnSubmit;
    
    private CameraHelper cameraHelper;
    private String photoPath;
    private String chipId;
    private String certNumber;
    private InitializationRepository initRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_cert_bind);
        
        // 获取传递的参数
        chipId = getIntent().getStringExtra(EXTRA_CHIP_ID);
        certNumber = getIntent().getStringExtra(EXTRA_CERT_NUMBER);
        
        initRepository = new InitializationRepository(this);
        initViews();
        initCamera();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        etPersonName = findViewById(R.id.et_person_name);
        etIdNumber = findViewById(R.id.et_id_number);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSubmit = findViewById(R.id.btn_submit);
        
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnCancel.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitBinding());
    }

    private void initCamera() {
        cameraHelper = new CameraHelper(this);
        cameraHelper.setCameraCallback(new CameraCallback() {
            @Override
            public void onPhotoTaken(String photoPath) {
                if (!TextUtils.isEmpty(photoPath)) {
                    PersonCertBindActivity.this.photoPath = photoPath;
                    // 显示预览，使用 fitCenter 保持图片完整显示
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    Glide.with(PersonCertBindActivity.this)
                            .load(photoPath)
                            .fitCenter()
                            .into(ivPhotoPreview);
                }
            }

            @Override
            public void onImageSelected(Uri imageUri) {
                // 不使用
            }
        });
    }

    private void takePhoto() {
        if (cameraHelper != null) {
            cameraHelper.takePhoto(true);
        }
    }

    private void submitBinding() {
        // 验证输入
        String personName = etPersonName.getText() != null ? etPersonName.getText().toString().trim() : "";
        String idNumber = etIdNumber.getText() != null ? etIdNumber.getText().toString().trim() : "";
        
//        if (TextUtils.isEmpty(personName)) {
//            Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (TextUtils.isEmpty(idNumber)) {
//            Toast.makeText(this, "请输入身份证号", Toast.LENGTH_SHORT).show();
//            return;
//        }
        
        if (TextUtils.isEmpty(photoPath)) {
            Toast.makeText(this, "请拍摄照片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取活动编码
        String activeCode = getActiveCode();
        if (TextUtils.isEmpty(activeCode)) {
            Toast.makeText(this, "获取活动编码失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 转换照片为Base64
        String base64Img = convertImageToBase64(photoPath);
        if (TextUtils.isEmpty(base64Img)) {
            Toast.makeText(this, "照片处理失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 构建请求数据
        BindCertDTO bindCertDTO = new BindCertDTO();
        bindCertDTO.activeCode = activeCode;
        bindCertDTO.certType = "2"; // 现场绑定实名证
        bindCertDTO.certNumber = certNumber != null ? certNumber : "";
        bindCertDTO.idNumber = idNumber;
        bindCertDTO.personName = personName;
        bindCertDTO.base64Img = base64Img;
        bindCertDTO.tagNo = chipId != null ? chipId : "";
        
        // 调用接口
        btnSubmit.setEnabled(false);
        btnSubmit.setText("提交中...");
        
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<String>> call = apiService.bindCert(bindCertDTO);
        
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("提交");
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<String> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Toast.makeText(PersonCertBindActivity.this, "绑定成功", Toast.LENGTH_SHORT).show();
                            // 绑定成功后，返回到人证核验页面
                            // 通过设置 result 为 RESULT_OK 并传递 flag，让详情页也关闭
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("bind_success", true);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            Toast.makeText(PersonCertBindActivity.this, 
                                    "绑定失败：" + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(PersonCertBindActivity.this, 
                                "绑定失败：服务器错误", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("提交");
                    Toast.makeText(PersonCertBindActivity.this, 
                            "绑定失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Bind cert failed", t);
                });
            }
        });
    }

    /**
     * 获取活动编码
     */
    private String getActiveCode() {
        return AppPreferences.getLastActiveId(this);
    }

    /**
     * 将图片转换为Base64字符串
     */
    private String convertImageToBase64(String imagePath) {
        try {
            FileInputStream fis = new FileInputStream(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            
            if (bitmap == null) {
                return null;
            }
            
            // 压缩图片
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            
            // 转换为Base64
            String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64;
        } catch (Exception e) {
            Log.e(TAG, "Convert image to base64 failed", e);
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (cameraHelper != null) {
            if (requestCode == CameraHelper.REQUEST_CODE_TAKE_PHOTO) {
                cameraHelper.handleTakePhotoResult(resultCode);
            } else if (requestCode == CameraHelper.REQUEST_CODE_SELECT_IMAGE) {
                cameraHelper.handleSelectImageResult(resultCode, data);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (initRepository != null) {
            initRepository.close();
        }
    }
}
