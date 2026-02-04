package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class Author {
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("avatar_urls")
    private AvatarUrls avatarUrls;

    public static class AvatarUrls {
        @SerializedName("96")
        private String small;

        @SerializedName("300")
        private String medium;

        public String getSmall() {
            return small;
        }

        public String getMedium() {
            return medium;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AvatarUrls getAvatarUrls() {
        return avatarUrls;
    }
}