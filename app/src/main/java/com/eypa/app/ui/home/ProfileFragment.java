package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eypa.app.R;
import com.eypa.app.utils.ThemeUtils;

public class ProfileFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.card_favorites).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "收藏功能待开发", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.card_history).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "历史功能待开发", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.profile_menu, menu);
        
        // 获取 Toolbar 图标颜色
        TypedValue typedValue = new TypedValue();
        requireActivity().getTheme().resolveAttribute(R.attr.toolbarIconTint, typedValue, true);
        int iconColor = typedValue.data;

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        if (darkModeItem != null) {
            darkModeItem.setIcon(isNightMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
            if (darkModeItem.getIcon() != null) {
                darkModeItem.getIcon().setTint(iconColor);
            }
        }

        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        if (settingsItem != null && settingsItem.getIcon() != null) {
            settingsItem.getIcon().setTint(iconColor);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dark_mode) {
            toggleDarkMode();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(requireContext(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleDarkMode() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        
        boolean newDarkModeState = !isNightMode;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("DarkMode", newDarkModeState);
        editor.putBoolean("DarkModeFollowSystem", false);
        editor.apply();

        isDarkMode = newDarkModeState;
        requireActivity().invalidateOptionsMenu();

        ThemeUtils.applyTheme(requireContext());
    }
}
