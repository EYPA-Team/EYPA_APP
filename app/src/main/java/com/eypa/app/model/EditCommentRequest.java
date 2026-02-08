package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class EditCommentRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("comment_id")
    private int commentId;

    @SerializedName("content")
    private String content;

    public EditCommentRequest(String token, int commentId, String content) {
        this.token = token;
        this.commentId = commentId;
        this.content = content;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
