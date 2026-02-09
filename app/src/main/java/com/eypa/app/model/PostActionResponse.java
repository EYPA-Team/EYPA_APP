package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class PostActionResponse {
    private int code;
    private String msg;
    private Data data;

    public boolean isSuccess() {
        return code == 200;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        @SerializedName("like_count")
        private int likeCount;
        @SerializedName("is_liked")
        private boolean isLiked;
        @SerializedName("favorite_count")
        private int favoriteCount;
        @SerializedName("is_favorited")
        private boolean isFavorited;

        public int getLikeCount() {
            return likeCount;
        }

        public boolean isLiked() {
            return isLiked;
        }

        public int getFavoriteCount() {
            return favoriteCount;
        }

        public boolean isFavorited() {
            return isFavorited;
        }
    }
}
