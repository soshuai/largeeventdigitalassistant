package com.largeevent.management;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.data.InitializationRepository;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.widget.CommonConfig;

import java.util.ArrayList;
import java.util.List;

public class CertificateListActivity extends AppCompatActivity {

    public static Intent newIntent(Context context, boolean isVehicle) {
        Intent intent = new Intent(context, CertificateListActivity.class);
        intent.putExtra(CommonConfig.EXTRA_IS_CAR, isVehicle);
        return intent;
    }

    private InitializationRepository repository;
    private boolean showVehicle;
    private final List<CertificateInfo> allCertificates = new ArrayList<>();
    private final List<CertificateInfo> filteredCertificates = new ArrayList<>();
    private CertificateAdapter adapter;

    private EditText etSearch;
    private Button btnSearch;
    private TextView tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showVehicle = getIntent().getBooleanExtra(CommonConfig.EXTRA_IS_CAR, false);
        setContentView(R.layout.activity_certificate_list);
        repository = new InitializationRepository(this);
        initToolbar();
        initViews();
        loadData();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
            toolbar.setTitle(showVehicle ? "车证详情" : "人证详情");
        }
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search_name);
        btnSearch = findViewById(R.id.btn_search);
        tvEmpty = findViewById(R.id.tv_empty);

        RecyclerView recyclerView = findViewById(R.id.recycler_certificates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CertificateAdapter();
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> filterAndDisplay());
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    filterAndDisplay();
                }
            }
        });
    }

    private void loadData() {
        allCertificates.clear();
        allCertificates.addAll(repository.getAllCertificates());
        filterAndDisplay();
    }

    private void filterAndDisplay() {
        filteredCertificates.clear();
        String query = etSearch.getText().toString().trim();
        for (CertificateInfo info : allCertificates) {
            if (isVehicleInfo(info) != showVehicle) {
                continue;
            }
            if (!TextUtils.isEmpty(query) && !containsIgnoreCase(info.name, query)) {
                continue;
            }
            filteredCertificates.add(info);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredCertificates.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) {
            return false;
        }
        return source.toLowerCase().contains(target.toLowerCase());
    }

    private boolean isVehicleInfo(@Nullable CertificateInfo info) {
        if (info == null) {
            return false;
        }
        String docType = info.documentType;
        if (!TextUtils.isEmpty(docType)) {
            if (docType.contains("车") || docType.toLowerCase().contains("vehicle")) {
                return true;
            }
        }
        String name = info.name;
        if (!TextUtils.isEmpty(name)) {
            if (name.contains("车") || name.contains("车辆")) {
                return true;
            }
        }
        String cardSerial = info.cardSerial;
        if (!TextUtils.isEmpty(cardSerial)) {
            boolean hasLetter = false;
            for (int i = 0; i < cardSerial.length(); i++) {
                if (Character.isLetter(cardSerial.charAt(i))) {
                    hasLetter = true;
                    break;
                }
            }
            if (hasLetter && cardSerial.length() >= 5) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.close();
        }
    }

    private class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder> {

        @NonNull
        @Override
        public CertificateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_certificate_info, parent, false);
            return new CertificateViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CertificateViewHolder holder, int position) {
            holder.bind(filteredCertificates.get(position));
        }

        @Override
        public int getItemCount() {
            return filteredCertificates.size();
        }

        class CertificateViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivIcon;
            private final TextView tvTitle;
            private final TextView tvName;
            private final TextView tvDocumentType;
            private final TextView tvChipId;
            private final TextView tvExtra;
            private final TextView tvValidity;
            private final TextView tvPermission;

            CertificateViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_certificate_icon);
                tvTitle = itemView.findViewById(R.id.tv_certificate_title);
                tvName = itemView.findViewById(R.id.tv_certificate_name);
                tvDocumentType = itemView.findViewById(R.id.tv_certificate_type);
                tvChipId = itemView.findViewById(R.id.tv_certificate_chip);
                tvExtra = itemView.findViewById(R.id.tv_certificate_extra);
                tvValidity = itemView.findViewById(R.id.tv_certificate_validity);
                tvPermission = itemView.findViewById(R.id.tv_certificate_permissions);
            }

            void bind(CertificateInfo info) {
                tvTitle.setText(showVehicle ? "车证信息" : "证件信息");
                ivIcon.setImageResource(showVehicle ? R.drawable.ic_card_vehicle : R.drawable.ic_card_person);
                tvName.setText("姓名：" + safe(info.name));
                tvDocumentType.setText(showVehicle
                        ? "车牌号：" + safe(info.cardSerial)
                        : "证件类型：" + safe(info.documentType));
                tvChipId.setText("芯片号：" + safe(info.chipId));
                String permissionText = info.areaPermissions.isEmpty()
                        ? "ALL"
                        : TextUtils.join("、", info.areaPermissions);
                tvExtra.setText("场馆权限：" + permissionText);
                tvValidity.setText("有效时间：" + safe(info.validFrom) + " ~ " + safe(info.validTo));
                tvPermission.setText("区域权限：" + permissionText);
            }

            private String safe(String value) {
                return TextUtils.isEmpty(value) ? "--" : value;
            }
        }
    }
}


