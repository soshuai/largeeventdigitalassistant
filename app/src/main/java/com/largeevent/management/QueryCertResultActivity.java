package com.largeevent.management;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.data.DevicePermissionHelper;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.CertificateInfo;

/**
 * 查询证件结果回显（仅展示，不上传核验记录）。
 */
public class QueryCertResultActivity extends AppCompatActivity {

    public static final String EXTRA_HAS_DATA = "extra_has_data";
    public static final String EXTRA_CERTIFICATE = "extra_certificate";

    private static final String TAG = "QueryCertResult";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_cert_result);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout layoutContent = findViewById(R.id.layout_content);
        TextView tvEmpty = findViewById(R.id.tv_empty);

        boolean hasData = getIntent().getBooleanExtra(EXTRA_HAS_DATA, false);
        CertificateInfo info = (CertificateInfo) getIntent().getSerializableExtra(EXTRA_CERTIFICATE);

        if (!hasData || info == null) {
            layoutContent.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        layoutContent.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ImageView ivPhoto = findViewById(R.id.iv_photo);
        TextView tvName = findViewById(R.id.tv_name);
        TextView tvCertType = findViewById(R.id.tv_cert_type);
        TextView tvCertNumber = findViewById(R.id.tv_cert_number);
        TextView tvValidTime = findViewById(R.id.tv_valid_time);
        TextView tvVenue = findViewById(R.id.tv_venue_permission);
        TextView tvZone = findViewById(R.id.tv_zone_permission);
        TextView tvArea = findViewById(R.id.tv_area_permission);

        tvName.setText("姓名：" + safe(info.name));
        String typeLabel = !TextUtils.isEmpty(info.identityDocumentType)
                ? info.identityDocumentType
                : info.documentType;
        tvCertType.setText("证件类型：" + safe(typeLabel));
        String number = !TextUtils.isEmpty(info.number)
                ? info.number
                : (!TextUtils.isEmpty(info.certId) ? info.certId : info.identityDocumentNumber);
        tvCertNumber.setText("证件编号：" + safe(number));
        if (TextUtils.isEmpty(info.validFrom) && TextUtils.isEmpty(info.validTo)) {
            tvValidTime.setText("有效时间：永久");
        } else {
            tvValidTime.setText("有效时间：" + safe(info.validFrom) + " ~ " + safe(info.validTo));
        }

        if (!TextUtils.isEmpty(info.photoUrl)) {
            Glide.with(this)
                    .load(info.photoUrl)
                    .placeholder(R.drawable.ic_face_placeholder)
                    .error(R.drawable.ic_face_placeholder)
                    .into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_face_placeholder);
        }

        try {
            InitializationRepository repository = new InitializationRepository(this);
            BasicInfo basicInfo = repository.getBasicInfo();
            repository.close();
            if (basicInfo != null) {
                String venueNames = DevicePermissionHelper.resolvePrivilegeDisplayNames(
                        info.venuePrivileges, basicInfo.getVenueInfoList());
                // 区域权限 ↔ areaPrivileges / personCertAreaList
                String areaNames = DevicePermissionHelper.resolvePrivilegeDisplayNames(
                        info.areaPrivileges, basicInfo.getPersonCertAreaList());
                // 分区权限 ↔ zonePrivileges / personCertZoneList
                String partitionNames = DevicePermissionHelper.resolvePrivilegeDisplayNames(
                        info.zonePrivileges, basicInfo.getPersonCertZoneList());
                tvVenue.setText("场馆权限：" + displayOrDash(venueNames));
                tvZone.setText("分区权限：" + displayOrDash(partitionNames));
                tvArea.setText("区域权限：" + displayOrDash(areaNames));
            } else {
                tvVenue.setText("场馆权限：" + safe(info.venuePrivileges));
                tvZone.setText("分区权限：" + safe(info.zonePrivileges));
                tvArea.setText("区域权限：" + safe(info.areaPrivileges));
            }
        } catch (Exception e) {
            Log.e(TAG, "map privileges failed", e);
            tvVenue.setText("场馆权限：" + safe(info.venuePrivileges));
            tvZone.setText("分区权限：" + safe(info.zonePrivileges));
            tvArea.setText("区域权限：" + safe(info.areaPrivileges));
        }
    }

    private static String safe(String value) {
        return TextUtils.isEmpty(value) ? "--" : value;
    }

    private static String displayOrDash(String value) {
        return TextUtils.isEmpty(value) ? "--" : value;
    }
}
