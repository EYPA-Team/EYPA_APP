package com.eypa.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "eypa_history.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_HISTORY = "history";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_POST_ID = "post_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_VIEW_COUNT = "view_count";
    public static final String COLUMN_LIKE_COUNT = "like_count";
    public static final String COLUMN_PUBLISH_DATE = "publish_date";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_HISTORY + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_POST_ID + " INTEGER UNIQUE, " +
            COLUMN_TITLE + " TEXT, " +
            COLUMN_IMAGE_URL + " TEXT, " +
            COLUMN_VIEW_COUNT + " INTEGER, " +
            COLUMN_LIKE_COUNT + " INTEGER, " +
            COLUMN_PUBLISH_DATE + " TEXT, " +
            COLUMN_TIMESTAMP + " INTEGER" +
            ");";

    public HistoryHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }
}
