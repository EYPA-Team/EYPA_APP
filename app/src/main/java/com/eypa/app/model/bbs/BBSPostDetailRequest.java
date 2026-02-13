package com.eypa.app.model.bbs;

import com.google.gson.annotations.SerializedName;

public class BBSPostDetailRequest {
    @SerializedName("post_id")
    private int postId;

    @SerializedName("token")
    private String token;

    public BBSPostDetailRequest(int postId, String token) {
        this.postId = postId;
        this.token = token;
    }
}
