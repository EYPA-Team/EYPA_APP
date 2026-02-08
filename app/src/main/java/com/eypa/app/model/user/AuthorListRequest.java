package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;

public class AuthorListRequest {
    @SerializedName("user_id")
    private int userId;
    @SerializedName("page")
    private int page;
    @SerializedName("token")
    private String token;

    public AuthorListRequest(int userId, int page) {
        this.userId = userId;
        this.page = page;
    }

    public AuthorListRequest(int userId, int page, String token) {
        this.userId = userId;
        this.page = page;
        this.token = token;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
