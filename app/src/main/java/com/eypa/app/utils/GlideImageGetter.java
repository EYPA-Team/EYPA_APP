package com.eypa.app.utils;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Animatable;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.lang.ref.WeakReference;

public class GlideImageGetter implements Html.ImageGetter {

    private final WeakReference<TextView> container;

    public GlideImageGetter(TextView textView) {
        this.container = new WeakReference<>(textView);
    }

    @Override
    public Drawable getDrawable(String source) {
        final UrlDrawable urlDrawable = new UrlDrawable();

        TextView textView = container.get();
        if (textView != null) {
            // 关键修改：移除 .asBitmap()，直接加载 Drawable
            Glide.with(textView.getContext())
                    .load(source)
                    .into(new DrawableTarget(urlDrawable, textView));
        }

        return urlDrawable;
    }

    private class DrawableTarget extends CustomTarget<Drawable> {
        private final UrlDrawable urlDrawable;
        private final WeakReference<TextView> textViewRef;

        public DrawableTarget(UrlDrawable urlDrawable, TextView textView) {
            this.urlDrawable = urlDrawable;
            this.textViewRef = new WeakReference<>(textView);
        }

        @Override
        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
            int width = resource.getIntrinsicWidth();
            int height = resource.getIntrinsicHeight();

            // 适当缩放表情大小 (例如放大 1.5 倍)
            float scale = 1.5f;
            resource.setBounds(0, 0, (int)(width * scale), (int)(height * scale));

            // 将加载好的资源设置给包装器
            urlDrawable.setDrawable(resource);
            urlDrawable.setBounds(0, 0, (int)(width * scale), (int)(height * scale));

            TextView textView = textViewRef.get();
            if (textView != null) {
                // 如果是 GIF 动图，需要设置回调以触发刷新
                if (resource instanceof Animatable) {
                    resource.setCallback(new Drawable.Callback() {
                        @Override
                        public void invalidateDrawable(@NonNull Drawable who) {
                            textView.invalidate(); // 通知 TextView 重绘
                        }

                        @Override
                        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
                            textView.postDelayed(what, when - System.currentTimeMillis());
                        }

                        @Override
                        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
                            textView.removeCallbacks(what);
                        }
                    });
                    ((Animatable) resource).start(); // 开始播放动画
                }

                // 重置文本以触发重新布局 (让图片显示出来)
                textView.setText(textView.getText());
            }
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholder) {
        }
    }

    /**
     * 一个 Drawable 包装器，负责持有实际加载的图片（可能是静态图，也可能是 GIF）
     */
    private static class UrlDrawable extends BitmapDrawable {
        private Drawable drawable;

        @SuppressWarnings("deprecation")
        public UrlDrawable() {
            super();
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public void draw(Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }
    }
}