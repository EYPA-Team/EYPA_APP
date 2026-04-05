package com.eypa.app.model.message;

import com.google.gson.annotations.SerializedName;

public class NotificationRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("tab")
    private String tab;

    @SerializedName("page")
    private int page;

    public NotificationRequest(String token, String tab, int page) {
        this.token = token;
        this.tab = tab;
        this.page = page;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
