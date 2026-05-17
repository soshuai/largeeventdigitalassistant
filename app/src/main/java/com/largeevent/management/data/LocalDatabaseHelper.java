package com.largeevent.management.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "large_event.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_INIT_DATA = "init_data";
    public static final String TABLE_VERIFICATION_RECORDS = "verification_records";

    private static final String SQL_CREATE_INIT_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_INIT_DATA + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "server_url TEXT NOT NULL, " +
                    "payload TEXT NOT NULL, " +
                    "created_at INTEGER NOT NULL" +
                    ");";

    private static final String SQL_CREATE_VERIFICATION_RECORDS_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_VERIFICATION_RECORDS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "type TEXT NOT NULL, " +
                    "chip_id TEXT NOT NULL, " +
                    "name TEXT, " +
                    "plate_number TEXT, " +
                    "description TEXT, " +
                    "result_type TEXT NOT NULL, " +
                    "is_success INTEGER NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "is_uploaded INTEGER NOT NULL DEFAULT 0, " +
                    "certificate_data TEXT" +
                    ");";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_INIT_TABLE);
        db.execSQL(SQL_CREATE_VERIFICATION_RECORDS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(SQL_CREATE_VERIFICATION_RECORDS_TABLE);
        }
    }
}


