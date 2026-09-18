package com.largeevent.management;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.largeevent.management.data.ActiveUserParser;
import com.largeevent.management.hardware.IdCardReaderHelper;
import com.largeevent.management.hardware.ScanHelper;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.network.ApiService;
import com.largeevent.management.network.NetworkManager;
import com.largeevent.management.network.dto.ActiveUserBaseVo;
import com.largeevent.management.network.dto.ApiResponse;

import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 查询证件：支持手动输入、身份证 RFID、二维码扫描，调用 getActiveUserByIdNumber 回显。
 */
public class QueryCertActivity extends AppCompatActivity {

    private static final String TAG = "QueryCertActivity";
    private static final Pattern ID_NUMBER_PATTERN =
            Pattern.compile("\\b([1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx])\\b");

    private EditText etName;
    private EditText etIdNumber;
    private TextView tvStatus;
    private Button btnReadIdCard;
    private Button btnScanQr;
    private Button btnSubmit;

    private ScanHelper scanHelper;
    private IdCardReaderHelper idCardReaderHelper;
    private String currentIdType = IdCardReaderHelper.DEFAULT_ID_TYPE;
    private boolean submitting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_cert);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.et_name);
        etIdNumber = findViewById(R.id.et_id_number);
        tvStatus = findViewById(R.id.tv_status);
        btnReadIdCard = findViewById(R.id.btn_read_id_card);
        btnScanQr = findViewById(R.id.btn_scan_qr);
        btnSubmit = findViewById(R.id.btn_submit);

        scanHelper = new ScanHelper(this);
        scanHelper.setListener(this::onScanBarcode);
        idCardReaderHelper = new IdCardReaderHelper(this);

        btnReadIdCard.setOnClickListener(v -> readIdCard());
        btnScanQr.setOnClickListener(v -> startQrScan());
        btnSubmit.setOnClickListener(v -> submitQuery());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanHelper != null) {
            scanHelper.open();
        }
    }

    @Override
    protected void onPause() {
        if (scanHelper != null) {
            scanHelper.stopScan();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (scanHelper != null) {
            scanHelper.close();
        }
        if (idCardReaderHelper != null) {
            idCardReaderHelper.close();
        }
        super.onDestroy();
    }

    private void readIdCard() {
        setStatus("正在识别身份证…");
        btnReadIdCard.setEnabled(false);
        idCardReaderHelper.startReadOnce(new IdCardReaderHelper.Callback() {
            @Override
            public void onSuccess(IdCardReaderHelper.IdCardResult result) {
                btnReadIdCard.setEnabled(true);
                if (!TextUtils.isEmpty(result.name)) {
                    etName.setText(result.name);
                }
                etIdNumber.setText(result.idNumber);
                currentIdType = TextUtils.isEmpty(result.idType)
                        ? IdCardReaderHelper.DEFAULT_ID_TYPE
                        : result.idType;
                setStatus("身份证识别成功");
                Toast.makeText(QueryCertActivity.this, "识别成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                btnReadIdCard.setEnabled(true);
                setStatus(message);
                Toast.makeText(QueryCertActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(String message) {
                setStatus(message);
            }
        });
    }

    private void startQrScan() {
        setStatus("请对准二维码扫描…");
        if (scanHelper != null) {
            scanHelper.scan();
        }
    }

    private void onScanBarcode(String barcode) {
        if (TextUtils.isEmpty(barcode)) {
            return;
        }
        ParsedIdentity parsed = parseScanContent(barcode);
        if (!TextUtils.isEmpty(parsed.name)) {
            etName.setText(parsed.name);
        }
        if (!TextUtils.isEmpty(parsed.idNumber)) {
            etIdNumber.setText(parsed.idNumber);
        } else {
            etIdNumber.setText(barcode.trim());
        }
        if (!TextUtils.isEmpty(parsed.idType)) {
            currentIdType = parsed.idType;
        } else {
            currentIdType = IdCardReaderHelper.DEFAULT_ID_TYPE;
        }
        setStatus("扫码成功");
        Toast.makeText(this, "扫码成功", Toast.LENGTH_SHORT).show();
    }

    private void submitQuery() {
        if (submitting) {
            return;
        }
        String idNumber = etIdNumber.getText() != null
                ? etIdNumber.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(idNumber)) {
            Toast.makeText(this, "请先输入或识别身份证号", Toast.LENGTH_SHORT).show();
            return;
        }
        String idType = TextUtils.isEmpty(currentIdType)
                ? IdCardReaderHelper.DEFAULT_ID_TYPE
                : currentIdType;

        submitting = true;
        btnSubmit.setEnabled(false);
        setStatus("正在查询…");

        ApiService apiService = NetworkManager.getInstance().getApiService();
        apiService.getActiveUserByIdNumber(idNumber, idType)
                .enqueue(new Callback<ApiResponse<List<ActiveUserBaseVo>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ActiveUserBaseVo>>> call,
                                           @NonNull Response<ApiResponse<List<ActiveUserBaseVo>>> response) {
                        submitting = false;
                        btnSubmit.setEnabled(true);
                        if (!response.isSuccessful() || response.body() == null) {
                            setStatus("查询失败");
                            openEmptyResult();
                            return;
                        }
                        ApiResponse<List<ActiveUserBaseVo>> body = response.body();
                        if (body.getCode() != 200) {
                            setStatus(TextUtils.isEmpty(body.getMessage()) ? "查询失败" : body.getMessage());
                            openEmptyResult();
                            return;
                        }
                        List<ActiveUserBaseVo> list = body.getData();
                        if (list == null || list.isEmpty() || list.get(0) == null) {
                            setStatus("暂无该证件信息");
                            openEmptyResult();
                            return;
                        }
                        ActiveUserBaseVo dto = list.get(0);
                        ActiveUserParser.normalizeActiveUserDto(dto);
                        CertificateInfo info = ActiveUserParser.buildCertificateInfo(dto, dto.chipid);
                        if (info == null) {
                            Log.w(TAG, "query ok but CertificateInfo parse failed, certId="
                                    + dto.certId + ", idNumber=" + dto.identityDocumentNumber);
                            setStatus("证件数据解析失败");
                            openEmptyResult();
                            return;
                        }
                        setStatus("查询成功");
                        Intent intent = new Intent(QueryCertActivity.this, QueryCertResultActivity.class);
                        intent.putExtra(QueryCertResultActivity.EXTRA_CERTIFICATE, info);
                        intent.putExtra(QueryCertResultActivity.EXTRA_HAS_DATA, true);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ActiveUserBaseVo>>> call,
                                          @NonNull Throwable t) {
                        submitting = false;
                        btnSubmit.setEnabled(true);
                        Log.e(TAG, "query failed", t);
                        setStatus("网络错误：" + t.getMessage());
                        Toast.makeText(QueryCertActivity.this,
                                "查询失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openEmptyResult() {
        Intent intent = new Intent(this, QueryCertResultActivity.class);
        intent.putExtra(QueryCertResultActivity.EXTRA_HAS_DATA, false);
        startActivity(intent);
    }

    private void setStatus(String text) {
        if (tvStatus != null) {
            tvStatus.setText(text == null ? "" : text);
        }
    }

    private static ParsedIdentity parseScanContent(String raw) {
        ParsedIdentity result = new ParsedIdentity();
        String content = raw == null ? "" : raw.trim();
        if (TextUtils.isEmpty(content)) {
            return result;
        }

        // 格式：pdaUser:姓名:证件类型:证件号码
        // 例：pdaUser:张三:169:21486418948324
        if (content.startsWith("pdaUser:") || content.startsWith("pdaUser：")) {
            String body = content.contains("：")
                    ? content.substring("pdaUser：".length())
                    : content.substring("pdaUser:".length());
            String[] parts = body.split("[:：]", -1);
            if (parts.length >= 3) {
                result.name = safeTrim(parts[0]);
                result.idType = safeTrim(parts[1]);
                result.idNumber = safeTrim(parts[2]);
                // 证件号中若再含分隔，拼回后面段
                if (parts.length > 3) {
                    StringBuilder idBuilder = new StringBuilder(result.idNumber);
                    for (int i = 3; i < parts.length; i++) {
                        idBuilder.append(parts[i]);
                    }
                    result.idNumber = idBuilder.toString().trim();
                }
                return result;
            }
        }

        if (content.startsWith("{") && content.endsWith("}")) {
            try {
                JSONObject json = new JSONObject(content);
                result.name = firstNonEmpty(
                        json.optString("name"),
                        json.optString("chineseName"),
                        json.optString("chinese_name"));
                result.idNumber = firstNonEmpty(
                        json.optString("idNumber"),
                        json.optString("identityDocumentNumber"),
                        json.optString("id_number"));
                result.idType = firstNonEmpty(
                        json.optString("idType"),
                        json.optString("identityDocumentType"),
                        json.optString("id_type"));
            } catch (Exception e) {
                Log.w(TAG, "parse json barcode failed", e);
            }
        }
        if (TextUtils.isEmpty(result.idNumber)) {
            Matcher matcher = ID_NUMBER_PATTERN.matcher(content);
            if (matcher.find()) {
                result.idNumber = matcher.group(1);
            } else if (content.matches("(?i)[0-9Xx]{15,18}")) {
                result.idNumber = content;
            }
        }
        return result;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (!TextUtils.isEmpty(v)) {
                return v.trim();
            }
        }
        return "";
    }

    private static class ParsedIdentity {
        String name;
        String idNumber;
        String idType;
    }
}
