package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class PostDetailRequest {
    @SerializedName("post_id")
    private int postId;
    private String token;

    public PostDetailRequest(int postId, String token) {
        this.postId = postId;
        this.token = token;
    }
}
