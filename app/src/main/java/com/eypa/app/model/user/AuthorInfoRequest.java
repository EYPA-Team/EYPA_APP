package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;

public class AuthorInfoRequest {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("token")
    private String token;

    public AuthorInfoRequest(int userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
