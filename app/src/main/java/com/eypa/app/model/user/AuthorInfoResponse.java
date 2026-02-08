package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class AuthorInfoResponse {
    @SerializedName("code")
    private int code;

    @SerializedName("msg")
    private String msg;

    @SerializedName("data")
    private Data data;

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
        @SerializedName("id")
        private int id;

        @SerializedName("base")
        private BaseInfo base;

        @SerializedName("profile")
        private ProfileInfo profile;

        @SerializedName("stats")
        private StatsInfo stats;

        @SerializedName("interaction")
        private InteractionInfo interaction;

        public int getId() {
            return id;
        }

        public BaseInfo getBase() {
            return base;
        }

        public ProfileInfo getProfile() {
            return profile;
        }

        public StatsInfo getStats() {
            return stats;
        }

        public InteractionInfo getInteraction() {
            return interaction;
        }
    }

    public static class BaseInfo {
        @SerializedName("name")
        private String name;

        @SerializedName("avatar")
        private String avatar;

        @SerializedName("level")
        private LevelInfo level;

        @SerializedName("vip")
        private VipInfo vip;

        public String getName() {
            return name;
        }

        public String getAvatar() {
            return avatar;
        }

        public LevelInfo getLevel() {
            return level;
        }

        public VipInfo getVip() {
            return vip;
        }
    }

    public static class LevelInfo {
        @SerializedName("index")
        private int index;

        @SerializedName("name")
        private String name;

        @SerializedName("icon")
        private String icon;

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public String getIcon() {
            return icon;
        }
    }

    public static class VipInfo {
        @SerializedName("level")
        private int level;

        @SerializedName("name")
        private String name;

        public int getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }
    }

    public static class ProfileInfo {
        @SerializedName("cover")
        private String cover;

        @SerializedName("desc")
        private String desc;

        @SerializedName("auth")
        private AuthInfo auth;

        @SerializedName("medals")
        private List<MedalInfo> medals;

        public String getCover() {
            return cover;
        }

        public String getDesc() {
            return desc;
        }

        public AuthInfo getAuth() {
            return auth;
        }

        public List<MedalInfo> getMedals() {
            return medals;
        }
    }

    public static class AuthInfo {
        @SerializedName("title")
        private String title;

        @SerializedName("desc")
        private String desc;

        public String getTitle() {
            return title;
        }

        public String getDesc() {
            return desc;
        }
    }

    public static class MedalInfo implements Serializable {
        @SerializedName("name")
        private String name;

        @SerializedName("icon")
        private String icon;

        @SerializedName("desc")
        private String desc;

        public String getName() {
            return name;
        }

        public String getIcon() {
            return icon;
        }

        public String getDesc() {
            return desc;
        }
    }

    public static class StatsInfo {
        @SerializedName("fans")
        private int fans;

        @SerializedName("follows")
        private int follows;

        @SerializedName("posts")
        private int posts;

        @SerializedName("comments")
        private int comments;

        @SerializedName("views")
        private int views;

        public int getFans() {
            return fans;
        }

        public int getFollows() {
            return follows;
        }

        public int getPosts() {
            return posts;
        }

        public int getComments() {
            return comments;
        }

        public int getViews() {
            return views;
        }
    }

    public static class InteractionInfo {
        @SerializedName("is_following")
        private boolean isFollowing;

        @SerializedName("is_me")
        private boolean isMe;

        public boolean isFollowing() {
            return isFollowing;
        }

        public boolean isMe() {
            return isMe;
        }
    }
}
