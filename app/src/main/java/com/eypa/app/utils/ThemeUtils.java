package com.eypa.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    public static void applyTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean followSystem = sharedPreferences.getBoolean("DarkModeFollowSystem", true);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);

        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode;

        if (followSystem) {
            newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else {
            if (isDarkMode) {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
            }
        }

        if (currentMode != newMode) {
            AppCompatDelegate.setDefaultNightMode(newMode);
        }
    }
}
