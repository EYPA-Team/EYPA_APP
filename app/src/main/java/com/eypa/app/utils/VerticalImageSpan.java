package com.eypa.app.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

public class VerticalImageSpan extends ImageSpan {

    public VerticalImageSpan(Drawable drawable) {
        super(drawable);
    }

    /**
     * 告诉 TextView 这个图片有多宽，以及它占据的高度
     */
    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        android.graphics.Rect rect = d.getBounds();

        if (fm != null) {
            Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
            int fontHeight = fmPaint.bottom - fmPaint.top;
            int drHeight = rect.bottom - rect.top;

            // 计算垂直居中所需的偏移量
            int top = drHeight / 2 - fontHeight / 4;
            int bottom = drHeight / 2 + fontHeight / 4;

            fm.ascent = -bottom;
            fm.top = -bottom;
            fm.bottom = top;
            fm.descent = top;
        }
        return rect.right;
    }

    /**
     * 绘制图片
     */
    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        Drawable b = getDrawable();
        canvas.save();

        // y 是文字的基线 (Baseline)
        // paint.getFontMetricsInt().descent + paint.getFontMetricsInt().ascent 是文字的高度范围（ascent是负数）
        // (descent + ascent) / 2 就是文字中心的偏移量

        int transY = y + (paint.getFontMetricsInt().descent + paint.getFontMetricsInt().ascent) / 2 - (b.getBounds().bottom - b.getBounds().top) / 2;

        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }
}