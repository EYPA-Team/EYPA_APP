package com.eypa.app.ui.detail;

import android.text.Spanned;
import android.text.TextUtils;

import com.eypa.app.ui.detail.model.ContentBlock;
import com.eypa.app.ui.detail.model.DownloadBlock;
import com.eypa.app.ui.detail.model.HeaderBlock;
import com.eypa.app.ui.detail.model.ImageBlock;
import com.eypa.app.ui.detail.model.QuoteBlock;
import com.eypa.app.ui.detail.model.TextBlock;
import com.eypa.app.utils.HtmlUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ContentParser {

    public static List<ContentBlock> parse(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return new ArrayList<>();
        }

        List<ContentBlock> blocks = new ArrayList<>();
        // 配置 Jsoup 不要在解析时自动添加 html/body 标签，尽量保持片段原样
        Document doc = Jsoup.parseBodyFragment(htmlContent);
        Elements bodyChildren = doc.body().children();

        for (Element element : bodyChildren) {
            parseElement(element, blocks);
        }

        return blocks;
    }

    /**
     * 递归解析元素，根据类型分发处理
     */
    private static void parseElement(Element element, List<ContentBlock> blocks) {
        String tagName = element.tagName().toLowerCase();
        String className = element.className();

        // 1. 忽略无用的空元素
        if (TextUtils.isEmpty(element.text()) && element.select("img").isEmpty() && !element.hasClass("file-download-box")) {
            // 如果既没有文字，也没有图片，也不是下载框，大概率是占位符，忽略
            return;
        }

        // 2. 处理下载框 (file-download-box)
        // 注意：下载框可能嵌套在其他 div 中，我们优先检查当前元素是否包含下载框
        // 如果当前元素本身就是下载框，或者它唯一的目的是包裹下载框
        if (className.contains("file-download-box")) {
            addDownloadBlock(element, blocks);
            return;
        } else {
            // 检查是否包含下载框 (处理嵌套情况，如 tinymce-hide)
            Elements downloadBoxes = element.select(".file-download-box");
            if (!downloadBoxes.isEmpty()) {
                for (Element box : downloadBoxes) {
                    addDownloadBlock(box, blocks);
                }
                // 如果这个元素只包含下载框，处理完就返回，不再作为文本显示
                // 简单判断：如果移除下载框后内容为空，则不再处理
                if (element.clone().select(".file-download-box").remove().text().trim().isEmpty()) {
                    return;
                }
            }
        }

        // 3. 处理引用块 (quote_q)
        if (className.contains("quote_q") || className.contains("quote-mce")) {
            // 引用块内的 HTML 需要保留格式 (比如里面的链接)
            Spanned spanned = HtmlUtils.fromHtml(element.html());
            if (spanned.length() > 0) {
                blocks.add(new QuoteBlock(spanned));
            }
            return;
        }

        // 4. 处理标题 (h1 - h6)
        if (tagName.matches("h[1-6]")) {
            int level = Integer.parseInt(tagName.substring(1));
            blocks.add(new HeaderBlock(element.text(), level));
            return;
        }

        // 5. 处理图片
        Elements images = element.select("img");
        if (!images.isEmpty()) {
            for (Element img : images) {
                String imageUrl = img.attr("data-src");
                if (TextUtils.isEmpty(imageUrl)) {
                    imageUrl = img.attr("src");
                }
                if (!TextUtils.isEmpty(imageUrl)) {
                    blocks.add(new ImageBlock(imageUrl));
                }
            }
            // 如果这个 p 标签只包含图片，则不再作为文本添加
            if (tagName.equals("p") && element.text().trim().isEmpty()) {
                return;
            }
        }

        // 6. 默认处理：作为普通文本块
        // 移除已经被提取的图片标签，避免占位
        Element textElement = element.clone();
        textElement.select("img").remove();
        textElement.select(".file-download-box").remove(); // 移除已处理的下载框

        String html = textElement.outerHtml();
        Spanned text = HtmlUtils.fromHtml(html);
        if (text.length() > 0) {
            blocks.add(new TextBlock(text));
        }
    }

    private static void addDownloadBlock(Element element, List<ContentBlock> blocks) {
        String fileName = element.select(".file-download-name").text();
        String fileType = element.select(".desc-left").text();
        String fileSize = element.select(".desc-right").text();
        Element linkElement = element.select("a.file-download-btn").first();
        String downloadUrl = "";

        if (linkElement != null) {
            downloadUrl = linkElement.attr("href");
            if (TextUtils.isEmpty(downloadUrl) || "javascript:;".equals(downloadUrl)) {
                String fileId = linkElement.attr("data-download-file");
                if (!TextUtils.isEmpty(fileId)) {
                    downloadUrl = "https://eqmemory.cn/?download=" + fileId;
                }
            }
        }
        blocks.add(new DownloadBlock(fileName, fileType, fileSize, downloadUrl));
    }
}