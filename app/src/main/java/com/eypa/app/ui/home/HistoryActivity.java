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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eypa.app.R;
import com.eypa.app.db.HistoryManager;
import com.eypa.app.model.ContentItem;
import com.eypa.app.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private HistoryAdapter adapter;
    private List<ContentItem> historyList;
    private TextView emptyView;
    private RecyclerView recyclerView;

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

        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.tv_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        historyList = HistoryManager.getInstance(this).getHistory();
        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        updateEmptyState();
    }

    private void updateEmptyState() {
        if (historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
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
                    historyList.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
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
