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
    @SerializedName("post_count")
    private String postCount;
    @SerializedName("favorite_count")
    private int favoriteCount;

    public static class VipInfo {
        private int level;
        private String name;

        public int getLevel() { return level; }
        public String getName() { return name; }
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
    public String getPostCount() { return postCount; }
    public int getFavoriteCount() { return favoriteCount; }
}
