package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class CommentsRequest {
    @SerializedName("post_id")
    private int postId;

    @SerializedName("page")
    private int page;

    @SerializedName("token")
    private String token;

    @SerializedName("type")
    private String type;

    @SerializedName("only_author")
    private Integer onlyAuthor;

    public CommentsRequest(int postId, int page) {
        this.postId = postId;
        this.page = page;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setOnlyAuthor(Integer onlyAuthor) {
        this.onlyAuthor = onlyAuthor;
    }
}
