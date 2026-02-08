package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;

public class FanItem {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("avatar")
    private String avatar;
    @SerializedName("level")
    private AuthorInfoResponse.LevelInfo level;
    @SerializedName("vip")
    private AuthorInfoResponse.VipInfo vip;
    @SerializedName("desc")
    private String desc;
    @SerializedName("is_following")
    private boolean isFollowing;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public AuthorInfoResponse.LevelInfo getLevel() {
        return level;
    }

    public void setLevel(AuthorInfoResponse.LevelInfo level) {
        this.level = level;
    }

    public AuthorInfoResponse.VipInfo getVip() {
        return vip;
    }

    public void setVip(AuthorInfoResponse.VipInfo vip) {
        this.vip = vip;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }
}
