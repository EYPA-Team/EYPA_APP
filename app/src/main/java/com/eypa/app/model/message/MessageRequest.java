package com.eypa.app.model.message;

import com.google.gson.annotations.SerializedName;

public class MessageRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("page")
    private int page;

    public MessageRequest(String token, int page) {
        this.token = token;
        this.page = page;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
