package com.eypa.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.eypa.app.model.Category;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryCacheManager {
    private static final String PREF_NAME = "CategoryCache";
    private static final String KEY_CATEGORIES = "categories";
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public CategoryCacheManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveCategories(List<Category> categories) {
        String json = gson.toJson(categories);
        sharedPreferences.edit().putString(KEY_CATEGORIES, json).apply();
    }

    public List<Category> getCategories() {
        String json = sharedPreferences.getString(KEY_CATEGORIES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Category>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void clearCache() {
        sharedPreferences.edit().remove(KEY_CATEGORIES).apply();
    }
}
