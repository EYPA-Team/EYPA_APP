package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class SubmitCommentRequest {
    private String token;
    @SerializedName("post_id")
    private int postId;
    private String content;
    @SerializedName("parent_id")
    private int parentId;

    public SubmitCommentRequest(String token, int postId, String content, int parentId) {
        this.token = token;
        this.postId = postId;
        this.content = content;
        this.parentId = parentId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
}
