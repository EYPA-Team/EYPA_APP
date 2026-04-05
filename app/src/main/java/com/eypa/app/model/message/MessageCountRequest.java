package com.eypa.app.model.message;

public class MessageCountRequest {
    private String token;

    public MessageCountRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
