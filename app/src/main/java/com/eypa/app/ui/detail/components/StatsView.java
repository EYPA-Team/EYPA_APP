package com.eypa.app.ui.detail.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eypa.app.R;

public class StatsView extends LinearLayout {

    private TextView viewCount;
    private TextView likeCount;
    private TextView commentCount;

    public StatsView(Context context) {
        super(context);
        init(context);
    }

    public StatsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_stats, this, true);
        viewCount = findViewById(R.id.view_count);
        likeCount = findViewById(R.id.like_count);
        commentCount = findViewById(R.id.comment_count);

        // --- 关键修改：在这里设置图标 ---
        // 这样可以确保图标颜色跟随主题的 textColorSecondary 变化
        viewCount.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_views, 0, 0, 0);
        likeCount.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_likes, 0, 0, 0);
        commentCount.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comments, 0, 0, 0);
    }

    public void setStats(int views, int likes, int comments) {
        viewCount.setText(String.valueOf(views));
        likeCount.setText(String.valueOf(likes));
        commentCount.setText(String.valueOf(comments));
    }
}