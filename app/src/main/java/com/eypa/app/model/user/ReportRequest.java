package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;

public class ReportRequest {
    @SerializedName("token")
    private String token;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("reason")
    private String reason;
    @SerializedName("desc")
    private String desc;
    @SerializedName("url")
    private String url;

    public ReportRequest(String token, int userId, String reason, String desc, String url) {
        this.token = token;
        this.userId = userId;
        this.reason = reason;
        this.desc = desc;
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
