package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Comment {

    private int id;
    private int parent;

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("author_avatar_urls")
    private Map<String, String> authorAvatarUrls;

    private String date;

    @SerializedName("content")
    private Content content;

    // 用于构建评论树的临时字段
    private transient List<Comment> children = new ArrayList<>();

    public static class Content {
        @SerializedName("rendered")
        private String rendered;

        public String getRendered() {
            return rendered;
        }
    }

    public int getId() {
        return id;
    }

    public int getParent() {
        return parent;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAvatarUrl() {
        if (authorAvatarUrls != null) {
            // 优先获取96尺寸的头像，如果不存在则获取任意一个
            if (authorAvatarUrls.containsKey("96")) {
                return authorAvatarUrls.get("96");
            } else if (!authorAvatarUrls.isEmpty()) {
                return authorAvatarUrls.values().iterator().next();
            }
        }
        return null;
    }

    public String getDate() {
        return date;
    }

    public Content getContent() {
        return content;
    }

    public List<Comment> getChildren() {
        return children;
    }

    public void setChildren(List<Comment> children) {
        this.children = children;
    }
}