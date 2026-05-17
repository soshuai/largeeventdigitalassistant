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
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
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
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
    private final SimpleDateFormat apiSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private Spinner spinnerRecordType;
    private Spinner spinnerResultType;
    private Spinner spinnerDateRange;
    private TextView btnReset;
    private TextView btnFilter;
    private TextView tvRecordCount;
    private RecyclerView recyclerRecords;

    private RecordAdapter adapter;
    private List<Object> allRecords = new ArrayList<>(); // 存储PersonCardCheckModel或CarCertificateCheckModel
    
    // 筛选条件
    private String selectedRecordType = "人证核验"; // null表示全部, "人证核验", "车证核验"
    private Integer selectedCheckStatus = null; // null表示全部, 0=失败, 1=成功
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

        // 隐藏上传按钮（接口查询不需要）
        findViewById(R.id.btn_upload).setVisibility(View.GONE);

        recyclerRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter();
        recyclerRecords.setAdapter(adapter);
        
        // 添加滚动监听，实现上拉加载
        recyclerRecords.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // 向上滚动
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                        
                        // 当滚动到底部时加载更多
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                                && firstVisibleItemPosition >= 0) {
                            loadMoreRecords();
                        }
                    }
                }
            }
        });

        // 重置按钮
        btnReset.setOnClickListener(v -> {
            selectedCheckStatus = null;
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
        selectedCheckStatus = resultTypePos == 0 ? null : (resultTypePos == 1 ? 1 : 0);

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
        pageDTO.current = currentPersonPage;
        pageDTO.size = 10L;
        pageDTO.checkStatus = selectedCheckStatus;
        
        // 根据日期范围设置 beginTime 和 endTime
        String[] dateRange = calculateDateRange(selectedDateRangePos);
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
                        
                        if (pageResult.records != null && !pageResult.records.isEmpty()) {
                            runOnUiThread(() -> {
                                allRecords.addAll(pageResult.records);
                                adapter.notifyDataSetChanged();
                                updateRecordCount();
                            });
                        } else {
                            // 没有更多数据
                            hasMorePersonRecords = false;
                        }
                        
                        // 检查是否还有更多数据
                        if (pageResult.records == null || pageResult.records.size() < pageDTO.size) {
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
        pageDTO.current = currentCarPage;
        pageDTO.size = 10L;
        pageDTO.checkStatus = selectedCheckStatus;
        
        // 根据日期范围设置 beginTime 和 endTime
        String[] dateRange = calculateDateRange(selectedDateRangePos);
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
                        
                        if (pageResult.records != null && !pageResult.records.isEmpty()) {
                            runOnUiThread(() -> {
                                allRecords.addAll(pageResult.records);
                                adapter.notifyDataSetChanged();
                                updateRecordCount();
                            });
                        } else {
                            // 没有更多数据
                            hasMoreCarRecords = false;
                        }
                        
                        // 检查是否还有更多数据
                        if (pageResult.records == null || pageResult.records.size() < pageDTO.size) {
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

    private void updateRecordCount() {
        long totalRecords = 0L;
        if ("人证核验".equals(selectedRecordType)) {
            totalRecords = personTotalRecords;
        } else if ("车证核验".equals(selectedRecordType)) {
            totalRecords = carTotalRecords;
        }
        tvRecordCount.setText("共" + totalRecords + "条记录");
    }

    /**
     * 根据日期范围位置计算 beginTime 和 endTime
     * @param dateRangePos 0=全部, 1=今天, 2=近7天, 3=近30天
     * @return String[]{beginTime, endTime}, 全部时返回 {null, null}
     */
    private String[] calculateDateRange(int dateRangePos) {
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
                result[0] = apiSdf.format(calendar.getTime());
                
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                result[1] = apiSdf.format(calendar.getTime());
            } else if (dateRangePos == 2) {
                // 近7天：7天前 00:00:00 到现在
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                result[0] = apiSdf.format(calendar.getTime());
                result[1] = apiSdf.format(endDate);
            } else if (dateRangePos == 3) {
                // 近30天：30天前 00:00:00 到现在
                calendar.add(Calendar.DAY_OF_YEAR, -30);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                result[0] = apiSdf.format(calendar.getTime());
                result[1] = apiSdf.format(endDate);
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
            private final TextView tvRecordChip;
            private final TextView tvRecordTypeTag;
            private final ImageView ivResultIcon;
            private final TextView tvResultText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivRecordIcon = itemView.findViewById(R.id.iv_record_icon);
                tvRecordTime = itemView.findViewById(R.id.tv_record_time);
                tvRecordName = itemView.findViewById(R.id.tv_record_name);
                tvRecordChip = itemView.findViewById(R.id.tv_record_chip);
                tvRecordTypeTag = itemView.findViewById(R.id.tv_record_type_tag);
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
                // 时间
                if (record.passTime != null) {
                    tvRecordTime.setText(sdf.format(record.passTime));
                } else {
                    tvRecordTime.setText("--");
                }

                // 图片：优先使用personPhoto，如果没有则使用默认图标
                if (!TextUtils.isEmpty(record.personPhoto)) {
                    Glide.with(ivRecordIcon.getContext())
                            .load(record.personPhoto)
                            .placeholder(R.drawable.ic_face_placeholder)
                            .error(R.drawable.ic_face_placeholder)
                            .into(ivRecordIcon);
                } else {
                    ivRecordIcon.setImageResource(R.drawable.ic_face_placeholder);
                }
                
                // 姓名
                if (!TextUtils.isEmpty(record.personName)) {
                    tvRecordName.setText("姓名: " + record.personName);
                } else {
                    tvRecordName.setText("姓名: --");
                }

                // 芯片号
                tvRecordChip.setText("芯片号: " + (record.tagNo != null ? record.tagNo : "--"));

                // 记录类型标签
                tvRecordTypeTag.setText("人证核验");
                tvRecordTypeTag.setBackgroundColor(0xFF4A90E2); // 蓝色

                // 核验结果
                if (record.checkStatus != null && record.checkStatus == 1) {
                    ivResultIcon.setImageResource(R.drawable.ic_result_success);
                    tvResultText.setText("核验成功");
                    tvResultText.setTextColor(0xFF52C41A); // 绿色
                } else {
                    ivResultIcon.setImageResource(R.drawable.ic_result_error);
                    tvResultText.setText("核验失败");
                    tvResultText.setTextColor(0xFFFF4D4F); // 红色
                }
            }
            
            void bindCarRecord(CarCertificateCheckModel record) {
                // 时间
                if (record.passTime != null) {
                    tvRecordTime.setText(sdf.format(record.passTime));
                } else {
                    tvRecordTime.setText("--");
                }

                // 图标和车牌
                ivRecordIcon.setImageResource(R.drawable.ic_card_vehicle);
                if (!TextUtils.isEmpty(record.carPlate)) {
                    tvRecordName.setText("车牌号: " + record.carPlate);
                } else {
                    tvRecordName.setText("车牌号: --");
                }

                // 芯片号
                tvRecordChip.setText("芯片号: " + (record.tagNo1 != null ? record.tagNo1 : "--"));

                // 记录类型标签
                tvRecordTypeTag.setText("车证核验");
                tvRecordTypeTag.setBackgroundColor(0xFFFF9800); // 橙色

                // 核验结果
                if (record.checkStatus != null && record.checkStatus == 1) {
                    ivResultIcon.setImageResource(R.drawable.ic_result_success);
                    tvResultText.setText("核验成功");
                    tvResultText.setTextColor(0xFF52C41A); // 绿色
                } else {
                    ivResultIcon.setImageResource(R.drawable.ic_result_error);
                    tvResultText.setText("核验失败");
                    tvResultText.setTextColor(0xFFFF4D4F); // 红色
                }
            }
        }
    }
}
