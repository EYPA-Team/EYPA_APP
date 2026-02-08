package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Comment {

    private int id;
    @SerializedName("post_id")
    private int postId;
    @SerializedName("parent_id")
    private int parentId;
    @SerializedName("is_topping")
    private boolean isTopping;
    private Author author;
    private Content content;
    private String date;
    @SerializedName("date_formatted")
    private String dateFormatted;
    private Object location;
    @SerializedName("reply_to")
    private ReplyTo replyTo;
    private Interaction interaction;
    private List<Comment> children;

    public String getAuthorName() {
        return author != null ? author.getName() : "";
    }

    public String getAvatarUrl() {
        return author != null ? author.getAvatar() : null;
    }

    public int getParent() {
        return parentId;
    }

    public int getId() {
        return id;
    }

    public int getPostId() {
        return postId;
    }

    public int getParentId() {
        return parentId;
    }

    public boolean isTopping() {
        return isTopping;
    }

    public Author getAuthor() {
        return author;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public String getDateFormatted() {
        return dateFormatted;
    }

    public Object getLocation() {
        return location;
    }

    public ReplyTo getReplyTo() {
        return replyTo;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public void setInteraction(Interaction interaction) {
        this.interaction = interaction;
    }

    public List<Comment> getChildren() {
        return children;
    }

    public void setChildren(List<Comment> children) {
        this.children = children;
    }

    public static class Author {
        private int id;
        private String name;
        private String avatar;
        private Level level;
        private Vip vip;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAvatar() {
            return avatar;
        }

        public Level getLevel() {
            return level;
        }

        public Vip getVip() {
            return vip;
        }
    }

    public static class Level {
        private Object index;
        private String name;
        private String icon;

        public int getIndex() {
            if (index instanceof Number) {
                return ((Number) index).intValue();
            } else if (index instanceof String) {
                try {
                    return Integer.parseInt((String) index);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            return 0;
        }

        public String getName() {
            return name;
        }

        public String getIcon() {
            return icon;
        }
    }

    public static class Vip {
        private int level;
        private String name;
    }

    public static class Content {
        private String rendered;
        private String raw;

        public String getRendered() {
            return rendered;
        }

        public void setRendered(String rendered) {
            this.rendered = rendered;
        }

        public String getRaw() {
            return raw;
        }

        public void setRaw(String raw) {
            this.raw = raw;
        }
    }

    public static class ReplyTo {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Interaction {
        @SerializedName("like_count")
        private int likeCount;
        @SerializedName("is_liked")
        private boolean isLiked;
        @SerializedName("reply_count")
        private int replyCount;

        public int getLikeCount() {
            return likeCount;
        }

        public boolean isLiked() {
            return isLiked;
        }

        public int getReplyCount() {
            return replyCount;
        }

        public void setLikeCount(int likeCount) {
            this.likeCount = likeCount;
        }

        public void setLiked(boolean liked) {
            isLiked = liked;
        }
    }
}