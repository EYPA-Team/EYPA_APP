package com.eypa.app.model.bbs;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BBSPost {
    private int id;
    private Title title;
    private Content content;
    private String date;
    @SerializedName("date_human")
    private String dateHuman;
    private String link;
    @SerializedName("author_info")
    private AuthorInfo authorInfo;
    private Plate plate;
    private Media media;
    private Stats stats;
    private Interaction interaction;
    private Permission permission;
    @SerializedName("bbs_info")
    private BBSInfo bbsInfo;

    public int getId() { return id; }
    public String getTitle() { return title != null ? title.rendered : ""; }
    public Content getContent() { return content; }
    public String getDate() { return date; }
    public String getDateHuman() { return dateHuman; }
    public String getLink() { return link; }
    public AuthorInfo getAuthorInfo() { return authorInfo; }
    public Plate getPlate() { return plate; }
    public Media getMedia() { return media; }
    public Stats getStats() { return stats; }
    public Interaction getInteraction() { return interaction; }
    public Permission getPermission() { return permission; }
    public BBSInfo getBbsInfo() { return bbsInfo; }

    public static class Title {
        public String rendered;
    }

    public static class Content {
        public String rendered;
        @SerializedName("protected")
        public boolean isProtected;
    }

    public static class AuthorInfo {
        public String id;
        public String name;
        public String avatar;
        public Level level;
        public Vip vip;
        @SerializedName("is_following")
        public boolean isFollowing;

        public static class Level {
            public int index;
            public String name;
            public String icon;
        }

        public static class Vip {
            public int level;
            public String name;
        }
    }

    public static class Plate {
        public int id;
        public String name;
        public String icon;
        public String desc;
    }

    public static class Media {
        public String type;
        @SerializedName("video_url")
        public String videoUrl;
        @SerializedName("slide_urls")
        public List<String> slideUrls;
        @SerializedName("cover_image")
        public String coverImage;
    }

    public static class Stats {
        public int views;
        public int likes;
        public int favorites;
        public int replies;
    }

    public static class Interaction {
        @SerializedName("is_liked")
        public boolean isLiked;
        @SerializedName("is_favorited")
        public boolean isFavorited;
        @SerializedName("can_reply")
        public boolean canReply;
    }

    public static class Permission {
        @SerializedName("can_view")
        public boolean canView;
        public String message;
        public String type;
    }

    public static class BBSInfo {
        public String type;
        @SerializedName("is_sticky")
        public boolean isSticky;
        @SerializedName("is_essence")
        public boolean isEssence;
        public List<Topic> topics;
        public Question question;

        public static class Topic {
            public int id;
            public String name;
        }

        public static class Question {
            public int status;
            @SerializedName("reward_points")
            public int rewardPoints;
            @SerializedName("best_answer_id")
            public int bestAnswerId;
        }
    }
}
