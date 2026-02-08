package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;

public class FollowRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("target_id")
    private int targetId;

    public FollowRequest(String token, int targetId) {
        this.token = token;
        this.targetId = targetId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
}
