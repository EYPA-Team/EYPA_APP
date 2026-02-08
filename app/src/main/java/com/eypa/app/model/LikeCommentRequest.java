package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class LikeCommentRequest {
    private String token;
    @SerializedName("comment_id")
    private int commentId;

    public LikeCommentRequest(String token, int commentId) {
        this.token = token;
        this.commentId = commentId;
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
}
