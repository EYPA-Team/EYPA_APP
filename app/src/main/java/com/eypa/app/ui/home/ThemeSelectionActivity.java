package com.eypa.app.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.eypa.app.R;
import com.eypa.app.utils.ThemeUtils;

public class ThemeSelectionActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        applyCustomTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_selection);

        setupToolbar();
        setupThemeSelection();
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

    private void setupThemeSelection() {
        RadioGroup radioGroup = findViewById(R.id.theme_radio_group);
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);

        boolean isValidTheme = themeId == R.style.Theme_EYPA_APP ||
                themeId == R.style.Theme_EYPA_APP_Cyan ||
                themeId == R.style.Theme_EYPA_APP_Green ||
                themeId == R.style.Theme_EYPA_APP_Blue ||
                themeId == R.style.Theme_EYPA_APP_Red ||
                themeId == R.style.Theme_EYPA_APP_Purple;

        if (!isValidTheme) {
            themeId = R.style.Theme_EYPA_APP;
            sharedPreferences.edit().putInt("ThemeId", themeId).apply();
        }

        final int currentThemeId = themeId;

        if (currentThemeId == R.style.Theme_EYPA_APP) {
            radioGroup.check(R.id.theme_yellow);
        } else if (currentThemeId == R.style.Theme_EYPA_APP_Cyan) {
            radioGroup.check(R.id.theme_cyan);
        } else if (currentThemeId == R.style.Theme_EYPA_APP_Green) {
            radioGroup.check(R.id.theme_green);
        } else if (currentThemeId == R.style.Theme_EYPA_APP_Blue) {
            radioGroup.check(R.id.theme_blue);
        } else if (currentThemeId == R.style.Theme_EYPA_APP_Red) {
            radioGroup.check(R.id.theme_red);
        } else if (currentThemeId == R.style.Theme_EYPA_APP_Purple) {
            radioGroup.check(R.id.theme_purple);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedThemeId = R.style.Theme_EYPA_APP;
            if (checkedId == R.id.theme_cyan) {
                selectedThemeId = R.style.Theme_EYPA_APP_Cyan;
            } else if (checkedId == R.id.theme_green) {
                selectedThemeId = R.style.Theme_EYPA_APP_Green;
            } else if (checkedId == R.id.theme_blue) {
                selectedThemeId = R.style.Theme_EYPA_APP_Blue;
            } else if (checkedId == R.id.theme_red) {
                selectedThemeId = R.style.Theme_EYPA_APP_Red;
            } else if (checkedId == R.id.theme_purple) {
                selectedThemeId = R.style.Theme_EYPA_APP_Purple;
            }

            if (selectedThemeId != currentThemeId) {
                sharedPreferences.edit().putInt("ThemeId", selectedThemeId).apply();
                restartApp();
            }
        });
    }

    private void restartApp() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
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
