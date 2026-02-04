package com.eypa.app.ui.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.eypa.app.R;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        setAppTheme(isDarkMode);
        applyCustomTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        setupClickListeners();
    }

    private void setAppTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void applyCustomTheme() {
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.layout_theme_settings).setOnClickListener(v -> showThemeSelectionDialog());
        findViewById(R.id.layout_about).setOnClickListener(v -> showAboutDialog());
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"绿色 (默认)", "蓝色", "红色", "紫色"};
        int[] themeIds = {
                R.style.Theme_EYPA_APP,
                R.style.Theme_EYPA_APP_Blue,
                R.style.Theme_EYPA_APP_Red,
                R.style.Theme_EYPA_APP_Purple
        };

        int currentThemeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        int checkedItem = 0;
        for (int i = 0; i < themeIds.length; i++) {
            if (themeIds[i] == currentThemeId) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择主题色")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    int selectedThemeId = themeIds[which];
                    if (selectedThemeId != currentThemeId) {
                        sharedPreferences.edit().putInt("ThemeId", selectedThemeId).apply();
                        dialog.dismiss();
                        restartApp();
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void restartApp() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("关于应用")
                .setMessage("EYPA Reader v1.0\n\n马国记忆")
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
