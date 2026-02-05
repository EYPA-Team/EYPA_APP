package com.eypa.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eypa.app.model.ContentItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static HistoryManager instance;
    private HistoryHelper dbHelper;

    private HistoryManager(Context context) {
        dbHelper = new HistoryHelper(context.getApplicationContext());
    }

    public static synchronized HistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryManager(context);
        }
        return instance;
    }

    public void addHistory(ContentItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HistoryHelper.COLUMN_POST_ID, item.getId());
        values.put(HistoryHelper.COLUMN_TITLE, item.getTitle());
        values.put(HistoryHelper.COLUMN_IMAGE_URL, item.getBestImageUrl());
        values.put(HistoryHelper.COLUMN_VIEW_COUNT, item.getViewCount());
        values.put(HistoryHelper.COLUMN_LIKE_COUNT, item.getLikeCount());
        values.put(HistoryHelper.COLUMN_PUBLISH_DATE, item.getDate());
        values.put(HistoryHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());

        db.insertWithOnConflict(HistoryHelper.TABLE_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<ContentItem> getHistory() {
        List<ContentItem> historyList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(HistoryHelper.TABLE_HISTORY, null, null, null, null, null, HistoryHelper.COLUMN_TIMESTAMP + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ContentItem item = new ContentItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(HistoryHelper.COLUMN_POST_ID)));
                item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(HistoryHelper.COLUMN_TITLE)));
                item.setCoverImage(cursor.getString(cursor.getColumnIndexOrThrow(HistoryHelper.COLUMN_IMAGE_URL)));
                item.setViewCount(cursor.getInt(cursor.getColumnIndexOrThrow(HistoryHelper.COLUMN_VIEW_COUNT)));
                item.setLikeCount(cursor.getInt(cursor.getColumnIndexOrThrow(HistoryHelper.COLUMN_LIKE_COUNT)));
                item.setDate(cursor.getString(cursor.getColumnIndexOrThrow(HistoryHelper.COLUMN_PUBLISH_DATE)));
                
                historyList.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return historyList;
    }
    
    public void clearHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(HistoryHelper.TABLE_HISTORY, null, null);
        db.close();
    }
}
