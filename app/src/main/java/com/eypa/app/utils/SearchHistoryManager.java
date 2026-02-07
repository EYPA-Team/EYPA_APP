package com.eypa.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {

    private static final String PREF_NAME = "search_history_pref";
    private static final String KEY_HISTORY = "search_history";
    private static final int MAX_HISTORY_SIZE = 10;

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SearchHistoryManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<String> getHistory() {
        String json = sharedPreferences.getString(KEY_HISTORY, "");
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void addHistory(String keyword) {
        if (TextUtils.isEmpty(keyword)) return;

        List<String> history = getHistory();
        
        history.remove(keyword);
        
        history.add(0, keyword);

        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(0, MAX_HISTORY_SIZE);
        }

        saveHistory(history);
    }

    public void clearHistory() {
        sharedPreferences.edit().remove(KEY_HISTORY).apply();
    }

    public void removeHistory(String keyword) {
        List<String> history = getHistory();
        if (history.remove(keyword)) {
            saveHistory(history);
        }
    }

    private void saveHistory(List<String> history) {
        String json = gson.toJson(history);
        sharedPreferences.edit().putString(KEY_HISTORY, json).apply();
    }
}
