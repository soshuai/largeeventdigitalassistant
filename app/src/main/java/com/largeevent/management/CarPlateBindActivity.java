package com.largeevent.management;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.data.VehicleBindingStore;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.CarCertificateDTO;
import com.largeevent.management.network.dto.UpdateCardInfoDTO;

import com.largeevent.management.network.dto.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CarPlateBindActivity extends AppCompatActivity {

    public static final String EXTRA_CERT_INFO = "extra_cert_info";
    public static final String EXTRA_CHIP_ID = "extra_chip_id";
    public static final String EXTRA_CAR_DTO = "extra_car_dto";

    private CertificateInfo certificateInfo;
    private String chipId;
    private CarCertificateDTO carDTO;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_plate_bind);
        certificateInfo = (CertificateInfo) getIntent().getSerializableExtra(EXTRA_CERT_INFO);
        chipId = getIntent().getStringExtra(EXTRA_CHIP_ID);
        carDTO = (CarCertificateDTO) getIntent().getSerializableExtra(EXTRA_CAR_DTO);
        initToolbar();
        initViews();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        TextView tvName = findViewById(R.id.tv_bind_name);
        TextView tvChip = findViewById(R.id.tv_bind_chip);
        TextView tvValidity = findViewById(R.id.tv_bind_validity);
        EditText etPlate = findViewById(R.id.et_plate);
        EditText etOrganization = findViewById(R.id.et_organization);
        Button btnBack = findViewById(R.id.btn_bind_back);
        Button btnConfirm = findViewById(R.id.btn_bind_confirm);

        if (certificateInfo != null) {
            tvName.setText("单位：" + safe(certificateInfo.name));
            tvChip.setText("芯片号：" + safe(certificateInfo.chipId));
                    
            // 如果开始时间和结束时间都为空，显示"永久有效"
            if (TextUtils.isEmpty(certificateInfo.validFrom) && TextUtils.isEmpty(certificateInfo.validTo)) {
                tvValidity.setText("有效时间：永久");
            } else {
                tvValidity.setText("有效时间：" + safe(certificateInfo.validFrom)
                        + " ~ " + safe(certificateInfo.validTo));
            }
        }

        btnBack.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> {
            String plate = etPlate.getText().toString().trim();
            if (TextUtils.isEmpty(plate)) {
                Toast.makeText(this, "请输入车牌号", Toast.LENGTH_SHORT).show();
                return;
            }
            String organization = etOrganization.getText().toString().trim();
            if (TextUtils.isEmpty(organization)) {
                Toast.makeText(this, "请输入单位名称", Toast.LENGTH_SHORT).show();
                return;
            }
            // 调用接口更新车证信息
            bindVehiclePlate(organization,plate);
        });
    }
    
    /**
     * 调用接口绑定车牌
     */
    private void bindVehiclePlate(String organization, String plate) {
        // 获取芯片号
        String actualChipId = getActualChipId();

        if (carDTO == null) {
            // 如果没有 carDTO，使用本地绑定（兼容旧逻辑）
            VehicleBindingStore.bind(actualChipId, plate);
            showSuccessAndFinish(plate);
            return;
        }

        String activeId = AppPreferences.getLastActiveId(this);
        InitializationRepository repository = new InitializationRepository(this);
        String eventCode = AppPreferences.resolveEventCode(this, repository.getBasicInfo());
        repository.close();

        // 构建更新请求
        UpdateCardInfoDTO updateCardInfoDTO = new UpdateCardInfoDTO();
        updateCardInfoDTO.accId = carDTO.id;
        updateCardInfoDTO.chipId = carDTO.tagNo1;
        updateCardInfoDTO.eventCode = !TextUtils.isEmpty(eventCode) ? eventCode : carDTO.number;
        updateCardInfoDTO.identityDocumentNumber = plate;
        updateCardInfoDTO.unitCodeDesc = organization;
        updateCardInfoDTO.registrationNumber = carDTO.number;
        updateCardInfoDTO.activityId = activeId;

        // 调用接口
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<Void>> call = apiService.updateCardInfo(updateCardInfoDTO);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Void>> call,
                    @NonNull Response<ApiResponse<Void>> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()
                            && response.body() != null
                            && response.body().isSuccess()) {
                        VehicleBindingStore.bind(actualChipId, plate);
                        showSuccessAndFinish(plate);
                    } else {
                        String msg = response.body() != null
                                ? response.body().getMessage()
                                : String.valueOf(response.code());
                        Toast.makeText(CarPlateBindActivity.this,
                                "绑定失败：" + msg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(CarPlateBindActivity.this,
                            "网络错误：" + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 获取实际的芯片号
     */
    private String getActualChipId() {
        String actualChipId = chipId;
        if (TextUtils.isEmpty(actualChipId) && certificateInfo != null) {
            actualChipId = certificateInfo.chipId;
        }
        if (TextUtils.isEmpty(actualChipId) && carDTO != null) {
            actualChipId = carDTO.tagNo1;
        }
        return actualChipId;
    }
    
    /**
     * 显示成功并关闭页面
     */
    private void showSuccessAndFinish(String plate) {
        String actualChipId = getActualChipId();
        
        VerificationResult result = new VerificationResult(
                VerificationResultType.PASS,
                "车牌绑定成功",
                "车牌绑定成功：" + plate,
                certificateInfo,
                false,
                null,
                true
        );
        Toast.makeText(this, "车牌绑定成功", Toast.LENGTH_SHORT).show();
        
        // 设置返回结果，通知详情页面绑定成功
        setResult(RESULT_OK);
        finish();
    }

    private String safe(String value) {
        return TextUtils.isEmpty(value) ? "--" : value;
    }
}


