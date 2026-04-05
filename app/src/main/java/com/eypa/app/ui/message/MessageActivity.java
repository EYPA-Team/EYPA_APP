package com.eypa.app.ui.message;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.eypa.app.R;
import com.eypa.app.utils.ThemeUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MessageActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private void applyCustomTheme() {
        int themeId = getSharedPreferences("AppSettings", MODE_PRIVATE).getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        applyCustomTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("消息");
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        setupViewPager();
    }

    private void setupViewPager() {
        String[] titles = new String[]{"未读", "评论", "点赞", "系统", "私信"};
        
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return NotificationFragment.newInstance("news");
                    case 1:
                        return NotificationFragment.newInstance("posts");
                    case 2:
                        return NotificationFragment.newInstance("like");
                    case 3:
                        return NotificationFragment.newInstance("system");
                    case 4:
                        return new PrivateMessageFragment();
                    default:
                        return NotificationFragment.newInstance("news");
                }
            }

            @Override
            public int getItemCount() {
                return titles.length;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(titles[position]);
        }).attach();
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
