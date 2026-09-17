package com.largeevent.management;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.data.AppPreferences;
import com.largeevent.management.image.ImageBase64Helper;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.*;
import com.largeevent.management.widget.CommonConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordActivity extends AppCompatActivity {

    private static final String TAG = "VerificationRecord";
    private final SimpleDateFormat apiDateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private Spinner spinnerRecordType;
    private Spinner spinnerResultType;
    private Spinner spinnerDateRange;
    private TextView btnReset;
    private TextView btnFilter;
    private TextView tvRecordCount;
    private RecyclerView recyclerRecords;
    private NestedScrollView nestedScrollView;

    private RecordAdapter adapter;
    private List<Object> allRecords = new ArrayList<>(); // 存储PersonCardCheckModel或CarCertificateCheckModel
    
    // 筛选条件
    private String selectedRecordType = "人证核验"; // null表示全部, "人证核验", "车证核验"
    /** null=全部；1=核验成功；0=核验失败 */
    private Integer selectedVerifyStatus = null;
    private int selectedDateRangePos = 0; // 0=全部, 1=今天, 2=近7天, 3=近30天
    
    // 分页参数
    private long currentPersonPage = 1L;
    private long currentCarPage = 1L;
    private boolean isLoadingPerson = false;
    private boolean isLoadingCar = false;
    private boolean hasMorePersonRecords = true;
    private boolean hasMoreCarRecords = true;
    
    // 总记录数
    private long personTotalRecords = 0L;
    private long carTotalRecords = 0L;
    private boolean showCar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        
        initToolbar();
        initViews();
        initSpinners();
        showCar = getIntent().getBooleanExtra(CommonConfig.EXTRA_IS_CAR, false);
        selectedRecordType = showCar?"车证核验":"人证核验";
        spinnerRecordType.setSelection(showCar?1:0);
        loadRecords();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        spinnerRecordType = findViewById(R.id.spinner_record_type);
        spinnerResultType = findViewById(R.id.spinner_result_type);
        spinnerDateRange = findViewById(R.id.spinner_date_range);
        btnReset = findViewById(R.id.btn_reset);
        btnFilter = findViewById(R.id.btn_filter);
        tvRecordCount = findViewById(R.id.tv_record_count);
        recyclerRecords = findViewById(R.id.recycler_records);
        nestedScrollView = findViewById(R.id.nested_scroll);

        // 隐藏上传按钮（接口查询不需要）
        findViewById(R.id.btn_upload).setVisibility(View.GONE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        recyclerRecords.setLayoutManager(layoutManager);
        recyclerRecords.setNestedScrollingEnabled(false);
        recyclerRecords.setHasFixedSize(false);
        adapter = new RecordAdapter();
        recyclerRecords.setAdapter(adapter);

        nestedScrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollY <= oldScrollY) {
                        return;
                    }
                    View child = v.getChildAt(0);
                    if (child == null) {
                        return;
                    }
                    if (scrollY >= child.getMeasuredHeight() - v.getMeasuredHeight() - 64) {
                        loadMoreRecords();
                    }
                });

        // 重置按钮
        btnReset.setOnClickListener(v -> {
            selectedVerifyStatus = null;
            selectedRecordType = showCar?"车证核验":"人证核验";
            selectedDateRangePos = 0;
            spinnerRecordType.setSelection(showCar?1:0);
            spinnerResultType.setSelection(0);
            spinnerDateRange.setSelection(0);
            loadRecords();
        });

        // 筛选按钮
        btnFilter.setOnClickListener(v -> applyFilter());
    }

    private void initSpinners() {
        // 记录类型
        ArrayAdapter<String> recordTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"人证核验", "车证核验"});
        recordTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerRecordType.setAdapter(recordTypeAdapter);

        // 核验结果
        ArrayAdapter<String> resultTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"全部", "核验成功", "核验失败"});
        resultTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerResultType.setAdapter(resultTypeAdapter);

        // 日期范围
        ArrayAdapter<String> dateRangeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"全部", "今天", "近7天", "近30天"});
        dateRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDateRange.setAdapter(dateRangeAdapter);
    }

    private void applyFilter() {
        int recordTypePos = spinnerRecordType.getSelectedItemPosition();
        selectedRecordType = recordTypePos == 0 ? "人证核验" : "车证核验";

        int resultTypePos = spinnerResultType.getSelectedItemPosition();
        selectedVerifyStatus = resultTypePos == 0 ? null : (resultTypePos == 1 ? 1 : 0);

        selectedDateRangePos = spinnerDateRange.getSelectedItemPosition();

        resetAndLoadRecords();
    }
    
    private void resetAndLoadRecords() {
        // 重置分页参数
        currentPersonPage = 1L;
        currentCarPage = 1L;
        hasMorePersonRecords = true;
        hasMoreCarRecords = true;
        personTotalRecords = 0L;
        carTotalRecords = 0L;
        
        allRecords.clear();
        adapter.notifyDataSetChanged();
        
        loadRecords();
    }

    private void loadRecords() {
        if ("人证核验".equals(selectedRecordType)) {
            loadPersonRecords();
        } else if ("车证核验".equals(selectedRecordType)) {
            loadCarRecords();
        }
    }
    
    private void loadMoreRecords() {
        if ("人证核验".equals(selectedRecordType)) {
            if (!isLoadingPerson && hasMorePersonRecords) {
                currentPersonPage++;
                loadPersonRecords();
            }
        } else if ("车证核验".equals(selectedRecordType)) {
            if (!isLoadingCar && hasMoreCarRecords) {
                currentCarPage++;
                loadCarRecords();
            }
        }
    }
    
    private void loadPersonRecords() {
        if (isLoadingPerson) return;
        isLoadingPerson = true;
        
        PersonCardCheckPageDTO pageDTO = new PersonCardCheckPageDTO();
        pageDTO.current = (int) currentPersonPage;
        pageDTO.size = 10;
        pageDTO.verifyStatus = selectedVerifyStatus;
        pageDTO.activityId = AppPreferences.getLastActiveId(this);
        if (TextUtils.isEmpty(pageDTO.activityId)) {
            pageDTO.activityId = null;
        }

        String[] dateRange = calculatePersonDateRange(selectedDateRangePos);
        pageDTO.beginTime = dateRange[0];
        pageDTO.endTime = dateRange[1];
        
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<PageResult<PersonCardCheckModel>>> call = apiService.selectPersonCheckRecord(pageDTO);
        
        call.enqueue(new Callback<ApiResponse<PageResult<PersonCardCheckModel>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PageResult<PersonCardCheckModel>>> call,
                                 @NonNull Response<ApiResponse<PageResult<PersonCardCheckModel>>> response) {
                isLoadingPerson = false;
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PageResult<PersonCardCheckModel>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        PageResult<PersonCardCheckModel> pageResult = apiResponse.getData();
                        
                        // 保存总记录数
                        personTotalRecords = pageResult.total;
                        
                        List<PersonCardCheckModel> records = copyPersonRecords(pageResult.records);
                        if (!records.isEmpty()) {
                            List<PersonCardCheckModel> finalRecords = records;
                            runOnUiThread(() -> {
                                allRecords.addAll(finalRecords);
                                adapter.notifyDataSetChanged();
                                updateRecordCount();
                            });
                        } else {
                            hasMorePersonRecords = false;
                        }

                        int rawSize = pageResult.records != null ? pageResult.records.size() : 0;
                        if (rawSize < pageDTO.size) {
                            hasMorePersonRecords = false;
                        }
                        
                        runOnUiThread(() -> updateRecordCount());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PageResult<PersonCardCheckModel>>> call, @NonNull Throwable t) {
                isLoadingPerson = false;
                Log.e(TAG, "Load person records failed", t);
            }
        });
    }
    
    private void loadCarRecords() {
        if (isLoadingCar) return;
        isLoadingCar = true;
        
        CarCertificateCheckPageDTO pageDTO = new CarCertificateCheckPageDTO();
        pageDTO.current = (int) currentCarPage;
        pageDTO.size = 10;
        pageDTO.verifyStatus = selectedVerifyStatus;
        pageDTO.activityId = AppPreferences.getLastActiveId(this);
        if (TextUtils.isEmpty(pageDTO.activityId)) {
            pageDTO.activityId = null;
        }

        String[] dateRange = calculatePersonDateRange(selectedDateRangePos);
        pageDTO.beginTime = dateRange[0];
        pageDTO.endTime = dateRange[1];
        
        ApiService apiService = NetworkManager.getInstance().getApiService();
        Call<ApiResponse<PageResult<CarCertificateCheckModel>>> call = apiService.selectCarCertCheckRecord(pageDTO);
        
        call.enqueue(new Callback<ApiResponse<PageResult<CarCertificateCheckModel>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PageResult<CarCertificateCheckModel>>> call,
                                 @NonNull Response<ApiResponse<PageResult<CarCertificateCheckModel>>> response) {
                isLoadingCar = false;
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PageResult<CarCertificateCheckModel>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        PageResult<CarCertificateCheckModel> pageResult = apiResponse.getData();
                        
                        // 保存总记录数
                        carTotalRecords = pageResult.total;
                        
                        List<CarCertificateCheckModel> records = copyCarRecords(pageResult.records);
                        if (!records.isEmpty()) {
                            List<CarCertificateCheckModel> finalRecords = records;
                            runOnUiThread(() -> {
                                allRecords.addAll(finalRecords);
                                adapter.notifyDataSetChanged();
                                updateRecordCount();
                            });
                        } else {
                            hasMoreCarRecords = false;
                        }

                        int rawSize = pageResult.records != null ? pageResult.records.size() : 0;
                        if (rawSize < pageDTO.size) {
                            hasMoreCarRecords = false;
                        }
                        
                        runOnUiThread(() -> updateRecordCount());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PageResult<CarCertificateCheckModel>>> call, @NonNull Throwable t) {
                isLoadingCar = false;
                Log.e(TAG, "Load car records failed", t);
            }
        });
    }

    private static List<PersonCardCheckModel> copyPersonRecords(
            @Nullable List<? extends PersonCardCheckModel> source) {
        List<PersonCardCheckModel> list = new ArrayList<>();
        if (source == null) {
            return list;
        }
        for (PersonCardCheckModel item : source) {
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    private static List<CarCertificateCheckModel> copyCarRecords(
            @Nullable List<? extends CarCertificateCheckModel> source) {
        List<CarCertificateCheckModel> list = new ArrayList<>();
        if (source == null) {
            return list;
        }
        for (CarCertificateCheckModel item : source) {
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    private void updateRecordCount() {
        long totalRecords = 0L;
        if ("人证核验".equals(selectedRecordType)) {
            totalRecords = personTotalRecords;
        } else if ("车证核验".equals(selectedRecordType)) {
            totalRecords = carTotalRecords;
        }
        tvRecordCount.setText("共" + totalRecords + "条记录");
    }

    /** 人证查询日期：yyyy-MM-dd */
    private String[] calculatePersonDateRange(int dateRangePos) {
        String[] result = calculateDateRange(dateRangePos, apiDateSdf);
        return result;
    }

    /**
     * 根据日期范围位置计算 beginTime 和 endTime
     * @param dateRangePos 0=全部, 1=今天, 2=近7天, 3=近30天
     * @return String[]{beginTime, endTime}, 全部时返回 {null, null}
     */
    private String[] calculateDateRange(int dateRangePos, SimpleDateFormat format) {
        String[] result = new String[2];
        
        if (dateRangePos == 0) {
            // 全部：不限制日期
            result[0] = null;
            result[1] = null;
        } else {
            Calendar calendar = Calendar.getInstance();
            Date endDate = calendar.getTime();
            
            if (dateRangePos == 1) {
                // 今天：00:00:00 到 23:59:59
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                result[0] = format.format(calendar.getTime());
                result[1] = format.format(endDate);
            } else if (dateRangePos == 2) {
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                result[0] = format.format(calendar.getTime());
                result[1] = format.format(endDate);
            } else if (dateRangePos == 3) {
                calendar.add(Calendar.DAY_OF_YEAR, -30);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                result[0] = format.format(calendar.getTime());
                result[1] = format.format(endDate);
            }
        }
        
        return result;
    }

    private class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_verification_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object record = allRecords.get(position);
            holder.bind(record);
        }

        @Override
        public int getItemCount() {
            return allRecords.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivRecordIcon;
            private final TextView tvRecordTime;
            private final TextView tvRecordName;
            private final TextView tvRecordChipLabel;
            private final TextView tvRecordChip;
            private final TextView tvRecordTypeTag;
            private final LinearLayout layoutStatusBadge;
            private final ImageView ivResultIcon;
            private final TextView tvResultText;
            private final RequestOptions avatarOptions = RequestOptions
                    .bitmapTransform(new RoundedCorners(12));

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivRecordIcon = itemView.findViewById(R.id.iv_record_icon);
                tvRecordTime = itemView.findViewById(R.id.tv_record_time);
                tvRecordName = itemView.findViewById(R.id.tv_record_name);
                tvRecordChipLabel = itemView.findViewById(R.id.tv_record_chip_label);
                tvRecordChip = itemView.findViewById(R.id.tv_record_chip);
                tvRecordTypeTag = itemView.findViewById(R.id.tv_record_type_tag);
                layoutStatusBadge = itemView.findViewById(R.id.layout_status_badge);
                ivResultIcon = itemView.findViewById(R.id.iv_result_icon);
                tvResultText = itemView.findViewById(R.id.tv_result_text);
            }

            void bind(Object record) {
                if (record instanceof PersonCardCheckModel) {
                    bindPersonRecord((PersonCardCheckModel) record);
                } else if (record instanceof CarCertificateCheckModel) {
                    bindCarRecord((CarCertificateCheckModel) record);
                }
            }
            
            void bindPersonRecord(PersonCardCheckModel record) {
                tvRecordTime.setText(TextUtils.isEmpty(record.verifyTime) ? "--" : record.verifyTime);
                loadPersonRecordPhoto(record);

                tvRecordName.setText(TextUtils.isEmpty(record.name) ? "--" : record.name);
                tvRecordChipLabel.setText("证件号");

                String certNumber = !TextUtils.isEmpty(record.idNumber)
                        ? record.idNumber
                        : record.cardNumber;
                tvRecordChip.setText(TextUtils.isEmpty(certNumber) ? "--" : certNumber);

                tvRecordTypeTag.setText("人证核验");
                tvRecordTypeTag.setBackgroundResource(R.drawable.bg_record_tag_person);
                tvRecordTypeTag.setTextColor(0xFF3B7DDD);

                applyResultStyle(isPersonVerifySuccess(record.verifyStatus),
                        isPersonVerifySuccess(record.verifyStatus)
                                ? "核验成功"
                                : verifyStatusLabel(record.verifyStatus));
            }

            private void loadPersonRecordPhoto(PersonCardCheckModel record) {
                String imageUrl = resolveRecordPhotoUrl(record.remark);
                if (TextUtils.isEmpty(imageUrl)) {
                    imageUrl = resolveRecordPhotoUrl(record.imageName);
                }
                if (!TextUtils.isEmpty(imageUrl)) {
                    Glide.with(ivRecordIcon.getContext())
                            .load(imageUrl)
                            .apply(avatarOptions)
                            .placeholder(R.drawable.ic_face_placeholder)
                            .error(R.drawable.ic_face_placeholder)
                            .centerCrop()
                            .into(ivRecordIcon);
                    return;
                }
                ivRecordIcon.setImageResource(R.drawable.ic_face_placeholder);
            }

            private boolean isPersonVerifySuccess(int verifyStatus) {
                return verifyStatus == 8 || verifyStatus == 1;
            }

            private void applyResultStyle(boolean success, String label) {
                if (success) {
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_record_status_success);
                    ivResultIcon.setImageResource(R.drawable.ic_result_success);
                    tvResultText.setText(label);
                    tvResultText.setTextColor(0xFF18A058);
                } else {
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_record_status_fail);
                    ivResultIcon.setImageResource(R.drawable.ic_result_error);
                    tvResultText.setText(label);
                    tvResultText.setTextColor(0xFFE54545);
                }
            }

            @Nullable
            private String resolveRecordPhotoUrl(@Nullable String source) {
                if (TextUtils.isEmpty(source)) {
                    return null;
                }
                String trimmed = source.trim();
                if (ImageBase64Helper.isHttpUrl(trimmed)) {
                    return trimmed;
                }
                return ImageBase64Helper.resolveAbsolutePhotoUrl(
                        trimmed, NetworkManager.getInstance().getCurrentBaseUrl());
            }

            private String verifyStatusLabel(int verifyStatus) {
                switch (verifyStatus) {
                    case 1: return "未识读出证件";
                    case 2: return "无效证件";
                    case 3: return "限制通行";
                    case 4: return "证件已注销";
                    case 5: return "证件未激活";
                    case 6: return "无权通行";
                    case 9: return "请检查证件";
                    default: return "核验失败";
                }
            }
            
            void bindCarRecord(CarCertificateCheckModel record) {
                tvRecordTime.setText(TextUtils.isEmpty(record.verifyTime) ? "--" : record.verifyTime);

                String imageUrl = resolveRecordPhotoUrl(record.remark);
                if (TextUtils.isEmpty(imageUrl)) {
                    imageUrl = resolveRecordPhotoUrl(record.imageName);
                }
                if (!TextUtils.isEmpty(imageUrl)) {
                    Glide.with(ivRecordIcon.getContext())
                            .load(imageUrl)
                            .apply(avatarOptions)
                            .placeholder(R.drawable.ic_card_vehicle)
                            .error(R.drawable.ic_card_vehicle)
                            .centerCrop()
                            .into(ivRecordIcon);
                } else {
                    ivRecordIcon.setImageResource(R.drawable.ic_card_vehicle);
                }

                tvRecordName.setText(TextUtils.isEmpty(record.name) ? "--" : record.name);
                tvRecordChipLabel.setText("芯片号");
                String chip = !TextUtils.isEmpty(record.cardNumber)
                        ? record.cardNumber
                        : record.idNumber;
                tvRecordChip.setText(TextUtils.isEmpty(chip) ? "--" : chip);

                tvRecordTypeTag.setText("车证核验");
                tvRecordTypeTag.setBackgroundResource(R.drawable.bg_record_tag_car);
                tvRecordTypeTag.setTextColor(0xFFE69500);

                applyResultStyle(isPersonVerifySuccess(record.verifyStatus),
                        isPersonVerifySuccess(record.verifyStatus)
                                ? "核验成功"
                                : verifyStatusLabel(record.verifyStatus));
            }
        }
    }
}
