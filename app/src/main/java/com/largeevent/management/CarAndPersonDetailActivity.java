package com.largeevent.management;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.camera.CameraCallback;
import com.largeevent.management.camera.CameraHelper;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.data.DevicePermissionHelper;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.ApiResponse;
import com.largeevent.management.network.dto.CarCertificateDTO;
import com.largeevent.management.network.dto.ReplacePhotoDTO;
import com.largeevent.management.widget.CommonConfig;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CarAndPersonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT = "extra_result";
    private static final String TAG = "VerificationDetail";
    private static final int REQUEST_CODE_PERSON_BIND = 1001;  // 人证绑定请求码
    private static final int REQUEST_CODE_CAR_BIND = 1002;  // 车证绑定请求码

    private ImageView ivResultIcon;
    private TextView tvResultTitle;
    private TextView tvResultDesc;
    private ImageView ivCertificateIcon;
    private ImageView ivPersonPhoto;  // 照片显示
    private Button btnChangePhoto;  // 更换头像按钮
    private TextView tvCertificateLabel;
    private TextView tvCertificateName;
    private TextView tvCertificateType;
    private TextView tvIdNumber;
    private TextView tvNumber;
    private TextView tvCertificateChip;
    private TextView tvCertificateValidity;
    private TextView tvVenuePermission;
    private TextView tvPartitionPermission;
    private TextView tvCertificatePermission;
    private TextView tvBindingAction;
    private VerificationResult currentResult;
    private InitializationRepository initRepository;
    private CarCertificateDTO carDTO;  // 保存车证DTO
    private CameraHelper cameraHelper;
    private String latestPhotoPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_detail);
        initToolbar();
        initViews();
        renderResult((VerificationResult) getIntent().getSerializableExtra(EXTRA_RESULT));
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        ivResultIcon = findViewById(R.id.iv_result_icon);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvResultDesc = findViewById(R.id.tv_result_desc);
        ivCertificateIcon = findViewById(R.id.iv_certificate_icon);
        ivPersonPhoto = findViewById(R.id.iv_person_photo);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        tvCertificateLabel = findViewById(R.id.tv_certificate_label);
        tvCertificateName = findViewById(R.id.tv_certificate_name);
        tvCertificateType = findViewById(R.id.tv_certificate_type);
        tvIdNumber = findViewById(R.id.tv_id_number);
        tvNumber = findViewById(R.id.tv_number);
        tvCertificateChip = findViewById(R.id.tv_certificate_chip);
        tvCertificateValidity = findViewById(R.id.tv_certificate_validity);
        tvVenuePermission = findViewById(R.id.tv_venue_permission);
        tvPartitionPermission = findViewById(R.id.tv_partition_permission);
        tvCertificatePermission = findViewById(R.id.tv_certificate_permission);
        tvBindingAction = findViewById(R.id.tv_binding_action);

        initRepository = new InitializationRepository(this);

        // 初始化相机助手
        cameraHelper = new CameraHelper(this);
        cameraHelper.setCameraCallback(new CameraCallback() {
            @Override
            public void onPhotoTaken(String photoPath) {
                if (!TextUtils.isEmpty(photoPath)) {
                    latestPhotoPath = photoPath;
                    // 不立即显示预览，等上传成功后再显示
                    // 直接上传照片
                    uploadPhoto();
                }
            }

            @Override
            public void onImageSelected(Uri imageUri) {
                // 不使用
            }
        });

        // 更换头像按钮点击事件
        btnChangePhoto.setOnClickListener(v -> {
            if (cameraHelper != null) {
                cameraHelper.takePhoto(true);
            }
        });

        Button btnBack = findViewById(R.id.btn_back);
        Button btnRetry = findViewById(R.id.btn_retry);
        btnBack.setOnClickListener(v -> finish());
        btnRetry.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("retry", true));
            finish();
        });
    }

    private void renderResult(@Nullable VerificationResult result) {
        if (result == null) {
            finish();
            return;
        }
        currentResult = result;

        // 获取 carDTO
        carDTO = (CarCertificateDTO) getIntent().getSerializableExtra("car_dto");
        tvResultTitle.setText(result.title);
        applyResultDescriptionStyle(result);
        if (result.type == VerificationResultType.PASS) {
            ivResultIcon.setImageResource(R.drawable.ic_result_success);
        } else {
            ivResultIcon.setImageResource(R.drawable.ic_result_error);
        }

        if (result.isVehicle()) {
            tvCertificateLabel.setText("车证信息");
            ivCertificateIcon.setImageResource(R.drawable.ic_card_vehicle);
        } else {
            tvCertificateLabel.setText("证件信息");
            ivCertificateIcon.setImageResource(R.drawable.ic_tab_person);
        }

        CertificateInfo info = result.certificateInfo;
        if (info != null) {
            // 显示图片：人证显示照片，车证显示车辆图标
            if (result.isVehicle()) {
                // 车证：显示本地车辆图标
                ivPersonPhoto.setVisibility(VISIBLE);
                ivPersonPhoto.setImageResource(R.drawable.ic_card_vehicle);
                btnChangePhoto.setVisibility(View.GONE);  // 车证不显示更换按钮
            } else if (!TextUtils.isEmpty(info.photoUrl)) {
                // 人证：显示 getActiveUser.verifyPhoto（URL 或 Base64）
                ivPersonPhoto.setVisibility(VISIBLE);
                loadCertificatePhoto(info.photoUrl);

                // 只有在核验失败且是人证不合一的情况下才显示更换按钮
                boolean isFaceMismatch = result.type == VerificationResultType.FACE_MISMATCH;
                btnChangePhoto.setVisibility(isFaceMismatch ? VISIBLE : View.GONE);
            } else {
                ivPersonPhoto.setVisibility(VISIBLE);
                ivPersonPhoto.setImageResource(R.drawable.ic_face_placeholder);
                btnChangePhoto.setVisibility(View.GONE);
            }

            tvCertificateName.setText(result.isVehicle() ? "单位：" + safe(info.name) : "姓名：" + safe(info.name));
            if (result.isVehicle()) {
                tvCertificateType.setText("车牌号码：" + safe(info.cardSerial));
                tvIdNumber.setVisibility(View.GONE);
            } else {
                tvCertificateType.setText("身份证类型：" + safe(
                        !TextUtils.isEmpty(info.identityDocumentType)
                                ? info.identityDocumentType
                                : info.documentType));
                tvIdNumber.setVisibility(VISIBLE);
                tvIdNumber.setText("身份证号码：" + safe(
                        !TextUtils.isEmpty(info.identityDocumentNumber)
                                ? info.identityDocumentNumber
                                : info.cardSerial));
            }
            tvCertificateChip.setText("芯片号：" + safe(info.chipId));
            tvNumber.setVisibility(VISIBLE);
            tvNumber.setText("编号：" + safe(info.certId));
            // 如果开始时间和结束时间都为空，显示"永久有效"
            if (TextUtils.isEmpty(info.validFrom) && TextUtils.isEmpty(info.validTo)) {
                tvCertificateValidity.setText("有效时间：永久");
            } else {
                tvCertificateValidity.setText("有效时间：" + safe(info.validFrom) + " ~ " + safe(info.validTo));
            }

            displayPermissions(info);
        } else {
            //            ivPersonPhoto.setVisibility(View.GONE);
            boolean showCar = getIntent().getBooleanExtra(CommonConfig.EXTRA_IS_CAR, false);
            if (showCar){
                tvCertificateName.setText("单位：--");
                tvCertificateType.setText("车牌号：--");
                tvNumber.setText("编号：--");
                tvNumber.setVisibility(VISIBLE);
            } else {
                tvCertificateName.setText("姓名：--");
                tvCertificateType.setText("身份证类型：--");
                tvIdNumber.setVisibility(VISIBLE);
                tvIdNumber.setText("身份证号码：--");
            }
            tvCertificateChip.setText("芯片号：--");
            tvNumber.setVisibility(VISIBLE);
            tvNumber.setText("编号：--");
            tvCertificateValidity.setText("有效时间：--");
            setPermissionPlaceholders();
        }

        if (result.isBindingRequired()) {
            tvBindingAction.setVisibility(VISIBLE);
            // 根据是否是车证设置不同的文字
            if (result.isVehicle()) {
                tvBindingAction.setText("车牌未绑定 去绑定>>");
                ivResultIcon.setImageResource(R.drawable.ic_result_success);
            } else {
                tvBindingAction.setText("证件信息未绑定 去绑定>>");
            }
            tvBindingAction.setOnClickListener(v -> {
                if (result.isVehicle()) {
                    // 车证绑定
                    Intent intent = new Intent(this, CarPlateBindActivity.class);
                    intent.putExtra(CarPlateBindActivity.EXTRA_CERT_INFO, currentResult.certificateInfo);
                    intent.putExtra(CarPlateBindActivity.EXTRA_CHIP_ID, currentResult.chipIdForBinding);

                    // 如果是车证绑定，传递 CarCertificateDTO
                    if (carDTO != null) {
                        intent.putExtra(CarPlateBindActivity.EXTRA_CAR_DTO, carDTO);
                    }

                    startActivityForResult(intent, REQUEST_CODE_CAR_BIND);
                } else {
                    // 人证绑定
                    Intent intent = new Intent(this, PersonCertBindActivity.class);
                    intent.putExtra(PersonCertBindActivity.EXTRA_CHIP_ID, currentResult.chipIdForBinding);
                    CertificateInfo certInfo = currentResult.certificateInfo;
                    if (certInfo != null) {
                        intent.putExtra(PersonCertBindActivity.EXTRA_CERT_NUMBER,
                                certInfo.identityDocumentNumber);
                        intent.putExtra(PersonCertBindActivity.EXTRA_ACC_ID, certInfo.certId);
                        intent.putExtra(PersonCertBindActivity.EXTRA_REGISTRATION_NUMBER, certInfo.number);
                        intent.putExtra(PersonCertBindActivity.EXTRA_VALID_BEGIN, certInfo.validFrom);
                        intent.putExtra(PersonCertBindActivity.EXTRA_VALID_END, certInfo.validTo);
                    }
                    startActivityForResult(intent, REQUEST_CODE_PERSON_BIND);
                }
            });
        } else {
            tvBindingAction.setVisibility(View.GONE);
        }
    }

    private String permissionString(CertificateInfo info) {
        if (info == null || info.areaPermissions.isEmpty()) {
            return "ALL";
        }
        return TextUtils.join("、", info.areaPermissions);
    }

    private void applyResultDescriptionStyle(VerificationResult result) {
        if (result.isVehicle() && result.type == VerificationResultType.PASS) {
            String plate = result.description;
            if (result.certificateInfo != null
                    && !TextUtils.isEmpty(result.certificateInfo.cardSerial)) {
                plate = result.certificateInfo.cardSerial.trim();
            }
            tvResultDesc.setText(TextUtils.isEmpty(plate) ? "—" : plate);
            tvResultDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            tvResultDesc.setTypeface(Typeface.DEFAULT_BOLD);
            tvResultDesc.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            tvResultDesc.setLetterSpacing(0.05f);
            return;
        }
        tvResultDesc.setText(result.description);
        tvResultDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvResultDesc.setTypeface(Typeface.DEFAULT);
        tvResultDesc.setTextColor(0xFF6B6F82);
        tvResultDesc.setLetterSpacing(0f);
    }

    /**
     * 加载证件照：支持 HTTP URL 与 data:image/jpeg;base64,... 或纯 Base64。
     */
    private void loadCertificatePhoto(@Nullable String photoSource) {
        if (TextUtils.isEmpty(photoSource)) {
            ivPersonPhoto.setImageResource(R.drawable.ic_face_placeholder);
            return;
        }
        String trimmed = photoSource.trim();
        if (trimmed.startsWith("data:image") || isLikelyBase64Image(trimmed)) {
            Bitmap bitmap = decodeBase64Photo(trimmed);
            if (bitmap != null) {
                ivPersonPhoto.setImageBitmap(bitmap);
                return;
            }
        }
        Glide.with(this)
                .load(trimmed)
                .placeholder(R.drawable.ic_face_placeholder)
                .error(R.drawable.ic_face_placeholder)
                .centerCrop()
                .into(ivPersonPhoto);
    }

    private static boolean isLikelyBase64Image(String value) {
        if (TextUtils.isEmpty(value) || value.startsWith("http")) {
            return false;
        }
        return value.length() > 64 && !value.contains(" ");
    }

    @Nullable
    private Bitmap decodeBase64Photo(String source) {
        try {
            String base64 = source;
            int comma = source.indexOf(',');
            if (comma >= 0) {
                base64 = source.substring(comma + 1);
            }
            byte[] bytes = Base64.decode(base64.trim(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Decode certificate photo failed", e);
            return null;
        }
    }

    private String safe(String value) {
        return TextUtils.isEmpty(value) ? "--" : value;
    }

    private void setPermissionPlaceholders() {
        tvVenuePermission.setText("场馆权限：--");
        tvPartitionPermission.setText("分区权限：--");
        tvCertificatePermission.setText("区域权限：--");
    }

    private String formatPermissionLine(String label, String displayNames) {
        if (TextUtils.isEmpty(displayNames)) {
            return label + "无权限";
        }
        return label + displayNames;
    }

    /**
     * 人证：展示 getActiveUser 返回的三类权限（code 转 name）；车证走原有逻辑。
     */
    private void displayPermissions(CertificateInfo info) {
        if (info == null) {
            setPermissionPlaceholders();
            return;
        }

        if (currentResult != null && currentResult.isVehicle() && carDTO != null) {
            tvPartitionPermission.setVisibility(View.GONE);
            displayCarPermissions(carDTO);
            return;
        }

        tvPartitionPermission.setVisibility(VISIBLE);
        try {
            BasicInfo basicInfo = initRepository.getBasicInfo();
            if (basicInfo == null) {
                setPermissionPlaceholders();
                return;
            }

            String venueNames = DevicePermissionHelper.resolvePrivilegeDisplayNames(
                    info.venuePrivileges, basicInfo.getVenueInfoList());
            String partitionNames = DevicePermissionHelper.resolvePrivilegeDisplayNames(
                    info.areaPrivileges, basicInfo.getPersonCertAreaList());
            String zoneNames = DevicePermissionHelper.resolvePrivilegeDisplayNames(
                    info.zonePrivileges, basicInfo.getPersonCertZoneList());

            tvVenuePermission.setText(formatPermissionLine("场馆权限：", venueNames));
            tvPartitionPermission.setText(formatPermissionLine("分区权限：", partitionNames));
            tvCertificatePermission.setText(formatPermissionLine("区域权限：", zoneNames));
        } catch (Exception e) {
            Log.e(TAG, "Failed to display permissions", e);
            tvVenuePermission.setText("场馆权限：解析失败");
            tvPartitionPermission.setText("分区权限：解析失败");
            tvCertificatePermission.setText("区域权限：解析失败");
        }
    }

    /**
     * 显示车证场馆权限和区域权限
     */
    private void displayCarPermissions(CarCertificateDTO carDTO) {
        if (carDTO == null) {
            setPermissionPlaceholders();
            return;
        }

        try {
            // 获取基础信息
            BasicInfo basicInfo = initRepository.getBasicInfo();
            if (basicInfo == null) {
                tvVenuePermission.setText("场馆权限：--");
                tvCertificatePermission.setText("区域权限：--");
                return;
            }

            // 获取车证的场馆权限 (area 字段) 和区域权限 (parkingArea 字段)
            String areaField = carDTO.area;  // 场馆权限
            String parkingAreaField = carDTO.parkingArea;  // 区域权限
            // 匹配场馆权限：根据 activeVenueModelList 的 venueCode 获取 venueName
            List<String> venueNames = new ArrayList<>();
            if (!TextUtils.isEmpty(areaField)) {
                String[] venueCodes = areaField.split(",");
                List<BasicInfo.ActiveVenueModel> venues = basicInfo.getActiveVenues();

                if (venues != null) {
                    for (String venueCode : venueCodes) {
                        String trimmedCode = venueCode.trim();
                        for (BasicInfo.ActiveVenueModel venue : venues) {
                            if (venue != null) {
                                if (trimmedCode.equals(venue.venueCode)) {
                                    String venueName = venue.venueName;
                                    if (TextUtils.isEmpty(venueName)) {
                                        venueName = venue.venueCode;
                                    }
                                    venueNames.add(venueName);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "最终匹配到的场馆名称: " + venueNames);

            // 显示场馆权限
            if (venueNames.isEmpty()) {
                tvVenuePermission.setText("场馆权限：无权限");
            } else {
                tvVenuePermission.setText("场馆权限：" + TextUtils.join("、", venueNames));
            }

            // 匹配区域权限：根据 carCertTypeList 的 dictCode 获取 dictValue
            List<String> areaNames = new ArrayList<>();
            if (!TextUtils.isEmpty(parkingAreaField)) {
                String[] areaCodes = parkingAreaField.split(",");
                List<BasicInfo.CertTypeModel> carCertTypes = basicInfo.getCarCertTypes();
                if (carCertTypes != null) {
                    for (String areaCode : areaCodes) {
                        String trimmedCode = areaCode.trim();
                        for (BasicInfo.CertTypeModel certType : carCertTypes) {
                            if (certType != null) {
                                if (trimmedCode.equals(certType.dictCode)) {
                                    String areaName = certType.dictValue;
                                    if (TextUtils.isEmpty(areaName)) {
                                        areaName = certType.dictCode;
                                    }
                                    areaNames.add(areaName);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // 显示区域权限
            if (areaNames.isEmpty()) {
                tvCertificatePermission.setText("区域权限：无权限");
            } else {
                tvCertificatePermission.setText("区域权限：" + TextUtils.join("、", areaNames));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to display car permissions", e);
            tvVenuePermission.setText("场馆权限:解析失败");
            tvCertificatePermission.setText("区域权限:解析失败");
        }
    }

    /**
     * 根据场馆代码获取场馆名称
     */
    private String getVenueNameByCode(BasicInfo basicInfo, String code) {
        if (basicInfo == null || TextUtils.isEmpty(code)) {
            return code;
        }

        List<BasicInfo.ActiveVenueModel> venues = basicInfo.getActiveVenues();
        if (venues != null) {
            for (BasicInfo.ActiveVenueModel venue : venues) {
                if (code.equals(venue.id)) {
                    return venue.venueName;
                }
            }
        }

        return code;  // 找不到名称就返回代码
    }

    /**
     * 根据区域代码获取区域名称
     */
    private String getAreaNameByCode(BasicInfo basicInfo, String code) {
        if (basicInfo == null || TextUtils.isEmpty(code)) {
            return code;
        }

        // 遍历所有场馆的区域列表
        List<BasicInfo.ActiveVenueModel> venues = basicInfo.getActiveVenues();
        if (venues != null) {
            for (BasicInfo.ActiveVenueModel venue : venues) {
                List<BasicInfo.ActiveAreaModel> areas = venue.getActiveAreaList();
                if (areas != null) {
                    for (BasicInfo.ActiveAreaModel area : areas) {
                        if (code.equals(area.areaCode)) {
                            return area.areaName;
                        }
                    }
                }
            }
        }

        return code;  // 找不到名称就返回代码
    }

    /**
     * 上传照片
     */
    private void uploadPhoto() {
        if (TextUtils.isEmpty(latestPhotoPath)) {
            Toast.makeText(this, "未找到照片文件", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentResult == null || currentResult.certificateInfo == null) {
            Toast.makeText(this, "证件信息不完整", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取证件ID
        String accId = currentResult.certificateInfo.certId;
        if (TextUtils.isEmpty(accId)) {
            Toast.makeText(this, "获取证件ID失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取活动编码
        String activeCode = getActiveCode();
        if (TextUtils.isEmpty(activeCode)) {
            Toast.makeText(this, "获取活动编码失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取活动ID
        String activityId = AppPreferences.getLastActiveId(this);

        // 转换照片为 Base64（带前缀）
        String base64Img = convertImageToBase64WithPrefix(latestPhotoPath);
        if (TextUtils.isEmpty(base64Img)) {
            Toast.makeText(this, "照片处理失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建请求数据
        ReplacePhotoDTO replacePhotoDTO = new ReplacePhotoDTO(accId, activeCode, base64Img);
        replacePhotoDTO.activityId = activityId;

        // 调用接口
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<String>> call = apiService.replacePhoto(replacePhotoDTO);

        // 显示加载中
        btnChangePhoto.setEnabled(false);
        btnChangePhoto.setText("上传中...");

        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                runOnUiThread(() -> {
                    btnChangePhoto.setEnabled(true);
                    btnChangePhoto.setText("更换头像");

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<String> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Toast.makeText(CarAndPersonDetailActivity.this, "照片更新成功", Toast.LENGTH_SHORT).show();
                            // 上传成功后才更新头像显示
                            if (!TextUtils.isEmpty(latestPhotoPath)) {
                                Glide.with(CarAndPersonDetailActivity.this).load(latestPhotoPath).placeholder(R.drawable.ic_face_placeholder).error(R.drawable.ic_face_placeholder).centerCrop().into(ivPersonPhoto);
                            }
                        } else {
                            Toast.makeText(CarAndPersonDetailActivity.this, "更新失败：" + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CarAndPersonDetailActivity.this, "更新失败：服务器错误", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    btnChangePhoto.setEnabled(true);
                    btnChangePhoto.setText("更换头像");
                    Toast.makeText(CarAndPersonDetailActivity.this, "更新失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Update photo failed", t);
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
     * 获取证件号码（优先使用身份证号，其次芯片号，最后证件号）
     */
    private String getCertNumber() {
        if (currentResult == null || currentResult.certificateInfo == null) {
            return null;
        }

        CertificateInfo info = currentResult.certificateInfo;
        //        // 优先使用 cardSerial（身份证号/证件号）
        //        if (!TextUtils.isEmpty(info.cardSerial)) {
        //            return info.cardSerial;
        //        }
        //        // 其次使用芯片号
        //        if (!TextUtils.isEmpty(info.chipId)) {
        return info.chipId;
        //        }
        //        return null;
    }

    /**
     * 将图片转换为 Base64 字符串（带 data:image/jpeg;base64, 前缀）
     */
    private String convertImageToBase64WithPrefix(String imagePath) {
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

            // 转换为 Base64（带前缀）
            String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64;
        } catch (Exception e) {
            Log.e(TAG, "Convert image to base64 failed", e);
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 处理绑定结果
        if (requestCode == REQUEST_CODE_PERSON_BIND && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("bind_success", false)) {
                // 绑定成功，关闭当前详情页，返回到人证核验页面
                finish();
                return;
            }
        }

        // 处理车证绑定结果
        if (requestCode == REQUEST_CODE_CAR_BIND && resultCode == RESULT_OK) {
            // 车牌绑定成功，关闭当前详情页，返回到车证核验页面
            finish();
            return;
        }

        // 处理相机结果
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
