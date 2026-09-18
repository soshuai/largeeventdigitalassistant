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
import com.largeevent.management.camera.CameraCallback;
import com.largeevent.management.camera.CameraHelper;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.UpdateCardInfoAo;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TP 日卡未绑定（bindStatus=0）时的现场实名绑定页，调用 updateCardInfo。
 */
public class PersonCertBindActivity extends AppCompatActivity {

    public static final String EXTRA_CHIP_ID = "extra_chip_id";
    public static final String EXTRA_CERT_NUMBER = "extra_cert_number";
    public static final String EXTRA_ACC_ID = "extra_acc_id";
    public static final String EXTRA_REGISTRATION_NUMBER = "extra_registration_number";
    public static final String EXTRA_VALID_BEGIN = "extra_valid_begin";
    public static final String EXTRA_VALID_END = "extra_valid_end";
    public static final String EXTRA_UNIT = "extra_unit";

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
    private String accId;
    private String registrationNumber;
    private String validBegin;
    private String validEnd;
    private String unitDesc;
    private InitializationRepository initRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_cert_bind);

        chipId = getIntent().getStringExtra(EXTRA_CHIP_ID);
        certNumber = getIntent().getStringExtra(EXTRA_CERT_NUMBER);
        accId = getIntent().getStringExtra(EXTRA_ACC_ID);
        registrationNumber = getIntent().getStringExtra(EXTRA_REGISTRATION_NUMBER);
        validBegin = getIntent().getStringExtra(EXTRA_VALID_BEGIN);
        validEnd = getIntent().getStringExtra(EXTRA_VALID_END);
        unitDesc = getIntent().getStringExtra(EXTRA_UNIT);

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

        if (!TextUtils.isEmpty(certNumber)) {
            etIdNumber.setText(certNumber);
        }

        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnCancel.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitBinding());
    }

    private void initCamera() {
        cameraHelper = new CameraHelper(this);
        cameraHelper.setCameraCallback(new CameraCallback() {
            @Override
            public void onPhotoTaken(String path) {
                if (!TextUtils.isEmpty(path)) {
                    photoPath = path;
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    Glide.with(PersonCertBindActivity.this)
                            .load(path)
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
        String personName = etPersonName.getText() != null ? etPersonName.getText().toString().trim() : "";
        String idNumber = etIdNumber.getText() != null ? etIdNumber.getText().toString().trim() : "";

//        if (TextUtils.isEmpty(idNumber)) {
//            Toast.makeText(this, "请输入身份证号", Toast.LENGTH_SHORT).show();
//            return;
//        }

        if (TextUtils.isEmpty(chipId)) {
            Toast.makeText(this, "芯片号缺失", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(photoPath)) {
            Toast.makeText(this, "请拍摄照片", Toast.LENGTH_SHORT).show();
            return;
        }

        BasicInfo basicInfo = initRepository.getBasicInfo();
        String eventCode = AppPreferences.resolveEventCode(this, basicInfo);
        if (TextUtils.isEmpty(eventCode)) {
            Toast.makeText(this, "获取活动代码失败", Toast.LENGTH_SHORT).show();
            return;
        }

        String verifyPhoto = convertImageToBase64WithPrefix(photoPath);
        if (TextUtils.isEmpty(verifyPhoto)) {
            Toast.makeText(this, "照片处理失败", Toast.LENGTH_SHORT).show();
            return;
        }

        String activityId = AppPreferences.getLastActiveId(this);

        UpdateCardInfoAo dto = new UpdateCardInfoAo();
        dto.accId = accId;
        dto.chipId = chipId;
        dto.eventCode = eventCode;
        dto.activityId = activityId;
        dto.name = personName;
        dto.unitCodeDesc = unitDesc;
        dto.verifyPhoto = verifyPhoto;
        dto.validBegin = validBegin;
        dto.validEnd = validEnd;
        dto.identityDocumentNumber = idNumber;
        dto.registrationNumber = !TextUtils.isEmpty(registrationNumber)
                ? registrationNumber
                : certNumber;

        btnSubmit.setEnabled(false);
        btnSubmit.setText("提交中...");

        ApiService apiService = NetworkManager.getInstance().getApiService();
        apiService.updateCardInfo(dto).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Void>> call,
                    @NonNull Response<ApiResponse<Void>> response) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("提交");

                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(PersonCertBindActivity.this,
                                "绑定失败：服务器错误", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(PersonCertBindActivity.this, "绑定成功", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("bind_success", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(PersonCertBindActivity.this,
                                "绑定失败：" + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("提交");
                    Toast.makeText(PersonCertBindActivity.this,
                            "绑定失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "updateCardInfo failed", t);
                });
            }
        });
    }

    private String convertImageToBase64WithPrefix(String imagePath) {
        try {
            FileInputStream fis = new FileInputStream(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            if (bitmap == null) {
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
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
