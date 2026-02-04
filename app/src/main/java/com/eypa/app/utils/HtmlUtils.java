package com.eypa.app.utils;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

    // 表情包的基础 URL
    private static final String SMILIE_BASE_URL = "https://eqmemory.cn/core/views/catfish/img/smilies/";

    /**
     * 专门用于评论区等需要显示表情包图片的地方。
     * 它会自动处理 [g=...] 标签，并使用 Glide 加载图片，且支持垂直居中。
     */
    public static void setHtmlText(TextView textView, String html) {
        if (html == null) {
            textView.setText("");
            return;
        }

        // 1. 处理自定义表情标签 [g=xxx/yyy.gif]
        String processedHtml = processCustomSmilies(html);

        // 2. 使用 GlideImageGetter 解析 img 标签得到初步的 Spanned
        Spanned spanned = HtmlCompat.fromHtml(
                processedHtml,
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                new GlideImageGetter(textView),
                null
        );

        // 3. --- 关键修复：将默认的 ImageSpan 替换为垂直居中的 VerticalImageSpan ---
        SpannableStringBuilder ssb;
        if (spanned instanceof SpannableStringBuilder) {
            ssb = (SpannableStringBuilder) spanned;
        } else {
            ssb = new SpannableStringBuilder(spanned);
        }

        ImageSpan[] imageSpans = ssb.getSpans(0, ssb.length(), ImageSpan.class);
        for (ImageSpan span : imageSpans) {
            int start = ssb.getSpanStart(span);
            int end = ssb.getSpanEnd(span);
            int flags = ssb.getSpanFlags(span);

            // 移除旧的，添加新的
            ssb.removeSpan(span);
            ssb.setSpan(new VerticalImageSpan(span.getDrawable()), start, end, flags);
        }

        textView.setText(ssb);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * 基础的 HTML 转 Spanned 方法。
     * 专门用于 ContentParser 解析文章正文的文本块。
     * 注意：这个方法不支持图片加载（因为没有 TextView 上下文）。
     */
    public static Spanned fromHtml(String html) {
        if (html == null) {
            return new SpannableString("");
        }
        // 仅仅做基础的 HTML 标签解析（如 <b>, <i> 等）
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    /**
     * 将 [g=group/name.gif] 转换为 <img src="URL">
     */
    private static String processCustomSmilies(String input) {
        // 正则表达式：匹配 [g=...]
        Pattern pattern = Pattern.compile("\\[g=(.*?)\\]");
        Matcher matcher = pattern.matcher(input);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            // matcher.group(1) 就是 "经典/fanu.gif"
            String path = matcher.group(1);
            String imgTag = "<img src=\"" + SMILIE_BASE_URL + path + "\" />";
            matcher.appendReplacement(sb, imgTag);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // 一个简单的空 Spannable 实现，防止 fromHtml 返回 null 导致空指针
    private static class SpannableString extends android.text.SpannableString {
        public SpannableString(CharSequence source) {
            super(source);
        }
    }
}