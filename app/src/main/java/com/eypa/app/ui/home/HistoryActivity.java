package com.eypa.app.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.eypa.app.R;
import com.eypa.app.db.HistoryManager;
import com.eypa.app.utils.ThemeUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("历史记录");
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @androidx.annotation.NonNull
            @Override
            public androidx.fragment.app.Fragment createFragment(int position) {
                return HistoryFragment.newInstance(position);
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("文章");
            } else {
                tab.setText("帖子");
            }
        }).attach();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.toolbarIconTint, typedValue, true);
        int iconColor = typedValue.data;
        
        MenuItem clearItem = menu.findItem(R.id.action_clear_history);
        if (clearItem != null && clearItem.getIcon() != null) {
            clearItem.getIcon().setTint(iconColor);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_clear_history) {
            showClearHistoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearHistoryDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("清空历史记录")
                .setMessage("确定要清空所有历史记录吗？")
                .setPositiveButton("确定", (d, which) -> {
                    HistoryManager.getInstance(this).clearHistory();
                    recreate();
                })
                .setNegativeButton("取消", null)
                .show();

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorPrimary);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(colorPrimary);
    }
}
