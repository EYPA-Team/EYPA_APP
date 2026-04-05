package com.eypa.app.model.message;

import com.google.gson.annotations.SerializedName;

public class ChatRecordRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("target_id")
    private int targetId;

    @SerializedName("page")
    private int page;

    public ChatRecordRequest(String token, int targetId, int page) {
        this.token = token;
        this.targetId = targetId;
        this.page = page;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
