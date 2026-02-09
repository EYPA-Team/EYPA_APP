package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class PostActionRequest {
    private String token;
    @SerializedName("post_id")
    private int postId;

    public PostActionRequest(String token, int postId) {
        this.token = token;
        this.postId = postId;
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
}
