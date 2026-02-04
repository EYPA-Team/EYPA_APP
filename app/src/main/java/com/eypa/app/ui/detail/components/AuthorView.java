package com.eypa.app.ui.detail.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;

public class AuthorView extends LinearLayout {

    private ImageView avatarView;
    private TextView nameView;

    public AuthorView(Context context) {
        super(context);
        init(context);
    }

    public AuthorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_author, this, true);
        avatarView = findViewById(R.id.author_avatar);
        nameView = findViewById(R.id.author_name);
    }

    // 修改参数类型为 ContentItem.Author
    public void setAuthor(ContentItem.Author author) {
        if (author == null) return;

        nameView.setText(author.getName());

        // 加载作者头像
        if (author.getAvatarUrls() != null && author.getAvatarUrls().getMedium() != null) {
            Glide.with(getContext())
                    .load(author.getAvatarUrls().getMedium())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(avatarView);
        } else {
            avatarView.setImageResource(R.drawable.ic_person);
        }
    }
}