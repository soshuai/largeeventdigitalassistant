package com.largeevent.management.record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.largeevent.management.data.LocalDatabaseHelper;
import com.largeevent.management.model.CertificateInfo;
import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;

import java.util.ArrayList;
import java.util.List;

public class VerificationRecordManager {

    private static final Gson gson = new Gson();

    /**
     * 添加记录到数据库
     */
    public static long addRecord(Context context, VerificationRecord record) {
        LocalDatabaseHelper helper = new LocalDatabaseHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("type", record.type);
        values.put("chip_id", record.chipId);
        values.put("name", record.name);
        values.put("plate_number", record.plateNumber);
        values.put("description", record.description);
        values.put("result_type", record.resultType.name());
        values.put("is_success", record.isSuccess ? 1 : 0);
        values.put("timestamp", record.timestamp);
        values.put("is_uploaded", record.isUploaded ? 1 : 0);
        
        // 序列化CertificateInfo
        if (record.certificateInfo != null) {
            values.put("certificate_data", gson.toJson(record.certificateInfo));
        }
        
        long id = db.insert(LocalDatabaseHelper.TABLE_VERIFICATION_RECORDS, null, values);
        db.close();
        return id;
    }

    /**
     * 从VerificationResult添加记录
     */
    public static long addRecord(Context context, String type, String chipId, VerificationResult result) {
        VerificationRecord record = VerificationRecord.fromResult(type, chipId, result);
        return addRecord(context, record);
    }

    /**
     * 获取所有记录
     */
    public static List<VerificationRecord> getRecords(Context context) {
        return getRecords(context, null, null, null, null);
    }

    /**
     * 获取记录（支持筛选）
     * @param recordType 记录类型：人证核验/车证核验，null表示全部
     * @param resultType 核验结果：成功/失败，null表示全部
     * @param startDate 开始日期时间戳，null表示不限制
     * @param endDate 结束日期时间戳，null表示不限制
     */
    public static List<VerificationRecord> getRecords(Context context, String recordType, 
                                                      Boolean resultType, Long startDate, Long endDate) {
        LocalDatabaseHelper helper = new LocalDatabaseHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        
        List<String> whereArgs = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder("1=1");
        
        if (!TextUtils.isEmpty(recordType)) {
            whereClause.append(" AND type = ?");
            whereArgs.add(recordType);
        }
        
        if (resultType != null) {
            whereClause.append(" AND is_success = ?");
            whereArgs.add(resultType ? "1" : "0");
        }
        
        if (startDate != null) {
            whereClause.append(" AND timestamp >= ?");
            whereArgs.add(String.valueOf(startDate));
        }
        
        if (endDate != null) {
            whereClause.append(" AND timestamp <= ?");
            whereArgs.add(String.valueOf(endDate));
        }
        
        Cursor cursor = db.query(
                LocalDatabaseHelper.TABLE_VERIFICATION_RECORDS,
                null,
                whereClause.toString(),
                whereArgs.toArray(new String[0]),
                null,
                null,
                "timestamp DESC"
        );
        
        List<VerificationRecord> records = new ArrayList<>();
        while (cursor.moveToNext()) {
            VerificationRecord record = cursorToRecord(cursor);
            records.add(record);
        }
        
        cursor.close();
        db.close();
        return records;
    }

    /**
     * 获取未上传的记录
     */
    public static List<VerificationRecord> getUnuploadedRecords(Context context) {
        LocalDatabaseHelper helper = new LocalDatabaseHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        
        Cursor cursor = db.query(
                LocalDatabaseHelper.TABLE_VERIFICATION_RECORDS,
                null,
                "is_uploaded = 0",
                null,
                null,
                null,
                "timestamp ASC"
        );
        
        List<VerificationRecord> records = new ArrayList<>();
        while (cursor.moveToNext()) {
            VerificationRecord record = cursorToRecord(cursor);
            records.add(record);
        }
        
        cursor.close();
        db.close();
        return records;
    }

    /**
     * 标记记录为已上传
     */
    public static void markAsUploaded(Context context, List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return;
        }
        
        LocalDatabaseHelper helper = new LocalDatabaseHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("is_uploaded", 1);
        
        StringBuilder placeholders = new StringBuilder();
        String[] args = new String[recordIds.size()];
        for (int i = 0; i < recordIds.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
            args[i] = String.valueOf(recordIds.get(i));
        }
        
        String whereClause = "id IN (" + placeholders.toString() + ")";
        
        db.update(LocalDatabaseHelper.TABLE_VERIFICATION_RECORDS, values, whereClause, args);
        db.close();
    }

    /**
     * 从Cursor创建VerificationRecord
     */
    private static VerificationRecord cursorToRecord(Cursor cursor) {
        Long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        String chipId = cursor.getString(cursor.getColumnIndexOrThrow("chip_id"));
        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        String plateNumber = cursor.getString(cursor.getColumnIndexOrThrow("plate_number"));
        String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        String resultTypeStr = cursor.getString(cursor.getColumnIndexOrThrow("result_type"));
        boolean isSuccess = cursor.getInt(cursor.getColumnIndexOrThrow("is_success")) == 1;
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
        boolean isUploaded = cursor.getInt(cursor.getColumnIndexOrThrow("is_uploaded")) == 1;
        
        VerificationResultType resultType;
        try {
            resultType = VerificationResultType.valueOf(resultTypeStr);
        } catch (Exception e) {
            resultType = VerificationResultType.INVALID_CERT;
        }
        
        CertificateInfo certificateInfo = null;
        String certData = cursor.getString(cursor.getColumnIndexOrThrow("certificate_data"));
        if (!TextUtils.isEmpty(certData)) {
            try {
                certificateInfo = gson.fromJson(certData, CertificateInfo.class);
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        VerificationRecord record = new VerificationRecord(id, type, chipId, name, plateNumber,
                description, resultType, isSuccess, timestamp, isUploaded, certificateInfo);
        return record;
    }

    /**
     * 获取记录总数
     */
    public static int getRecordCount(Context context, String recordType, Boolean resultType, 
                                     Long startDate, Long endDate) {
        List<VerificationRecord> records = getRecords(context, recordType, resultType, startDate, endDate);
        return records.size();
    }
}


