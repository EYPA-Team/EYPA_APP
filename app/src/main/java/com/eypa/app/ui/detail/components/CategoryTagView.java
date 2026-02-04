package com.eypa.app.ui.detail.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;

import com.eypa.app.R;
import com.eypa.app.model.Category;
import com.eypa.app.model.Tag;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;

import java.util.List;

public class CategoryTagView extends FlexboxLayout {

    public CategoryTagView(Context context) {
        super(context);
        init(context);
    }

    public CategoryTagView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CategoryTagView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 使用枚举值设置Flexbox属性
        setFlexDirection(FlexDirection.ROW);
        setFlexWrap(FlexWrap.WRAP);
        setAlignItems(AlignItems.CENTER);
        setPadding(0, dpToPx(8), 0, dpToPx(8));
    }

    public void setCategories(List<Category> categories) {
        removeAllViews();
        if (categories == null || categories.isEmpty()) return;

        for (Category category : categories) {
            addView(createCategoryTag(category.getName()));
        }
    }

    public void setTags(List<Tag> tags) {
        removeAllViews();
        if (tags == null || tags.isEmpty()) return;

        for (Tag tag : tags) {
            addView(createTagView(tag.getName()));
        }
    }

    private TextView createCategoryTag(String name) {
        TextView textView = (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.view_category_tag, this, false);
        textView.setText(name);
        textView.setBackgroundResource(R.drawable.bg_category_tag);

        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        Drawable background = textView.getBackground();
        if (background != null) {
            background = DrawableCompat.wrap(background.mutate());
            int alphaColor = ColorUtils.setAlphaComponent(colorPrimary, 204);
            DrawableCompat.setTint(background, alphaColor);
            textView.setBackground(background);
        }

        return textView;
    }

    private TextView createTagView(String name) {
        TextView textView = (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.view_tag, this, false);
        textView.setText("#" + name);
        textView.setBackgroundResource(R.drawable.bg_tag);
        return textView;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
