package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    private String token;
    private String id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String cover;
    private String desc;
    private int points;
    private int balance;
    private VipInfo vip;
    private LevelInfo level;
    @SerializedName("post_count")
    private String postCount;
    @SerializedName("favorite_count")
    private int favoriteCount;

    public static class VipInfo {
        private int level;
        private String name;

        public int getLevel() { return level; }
        public String getName() { return name; }

        public void setLevel(int level) { this.level = level; }
        public void setName(String name) { this.name = name; }
    }

    public static class LevelInfo {
        private int index;
        private String name;
        private String icon;

        public int getIndex() { return index; }
        public String getName() { return name; }
        public String getIcon() { return icon; }

        public void setIndex(int index) { this.index = index; }
        public void setName(String name) { this.name = name; }
        public void setIcon(String icon) { this.icon = icon; }
    }

    public String getToken() { return token; }
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getAvatar() { return avatar; }
    public String getCover() { return cover; }
    public String getDesc() { return desc; }
    public int getPoints() { return points; }
    public int getBalance() { return balance; }
    public VipInfo getVip() { return vip; }
    public LevelInfo getLevel() { return level; }
    public String getPostCount() { return postCount; }
    public int getFavoriteCount() { return favoriteCount; }

    public void setLevel(LevelInfo level) {
        this.level = level;
    }

    public void setVip(VipInfo vip) {
        this.vip = vip;
    }
}
