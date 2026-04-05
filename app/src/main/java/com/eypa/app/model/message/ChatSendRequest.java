package com.eypa.app.model.message;

import com.google.gson.annotations.SerializedName;

public class ChatSendRequest {
    private String token;

    @SerializedName("target_id")
    private int targetId;

    private String content;

    public ChatSendRequest(String token, int targetId, String content) {
        this.token = token;
        this.targetId = targetId;
        this.content = content;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
