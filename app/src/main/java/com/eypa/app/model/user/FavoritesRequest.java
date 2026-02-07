package com.eypa.app.model.user;

public class FavoritesRequest {
    private String token;
    private int page;

    public FavoritesRequest(String token, int page) {
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
