package com.eypa.app.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.eypa.app.R;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;

public class GeneralSettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        applyCustomTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);

        setupToolbar();
        setupViews();
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

    private void setupViews() {
        SwitchCompat switchUpdateDialog = findViewById(R.id.switch_update_dialog);
        
        boolean isUpdateEnabled = sharedPreferences.getBoolean("ShowUpdateDialog", true);
        switchUpdateDialog.setChecked(isUpdateEnabled);

        switchUpdateDialog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("ShowUpdateDialog", isChecked).apply();
        });

        findViewById(R.id.layout_update_dialog).setOnClickListener(v -> {
            switchUpdateDialog.toggle();
        });

        SwitchCompat switchDarkModeFollowSystem = findViewById(R.id.switch_dark_mode_follow_system);
        boolean isFollowSystem = sharedPreferences.getBoolean("DarkModeFollowSystem", true);
        switchDarkModeFollowSystem.setChecked(isFollowSystem);

        switchDarkModeFollowSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("DarkModeFollowSystem", isChecked).apply();
            ThemeUtils.applyTheme(this);
            getDelegate().applyDayNight();
        });

        findViewById(R.id.layout_dark_mode).setOnClickListener(v -> {
            switchDarkModeFollowSystem.toggle();
        });

        findViewById(R.id.layout_theme_settings).setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemeSelectionActivity.class);
            startActivity(intent);
        });

        View logoutLayout = findViewById(R.id.layout_logout);
        View logoutDivider = findViewById(R.id.divider_logout);
        UserManager.getInstance(this).isLoggedIn().observe(this, isLoggedIn -> {
            int visibility = isLoggedIn ? View.VISIBLE : View.GONE;
            logoutLayout.setVisibility(visibility);
            logoutDivider.setVisibility(visibility);
        });

        logoutLayout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    UserManager.getInstance(this).logout();
                    Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
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
