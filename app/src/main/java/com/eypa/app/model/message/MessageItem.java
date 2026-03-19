package com.eypa.app.model.message;

import com.google.gson.annotations.SerializedName;

public class MessageItem {
    @SerializedName("target_user")
    private TargetUser targetUser;

    @SerializedName("last_message")
    private String lastMessage;

    @SerializedName("last_time")
    private String lastTime;

    @SerializedName("date_human")
    private String dateHuman;

    @SerializedName("has_unread")
    private boolean hasUnread;

    @SerializedName("is_blacklist")
    private boolean isBlacklist;

    public TargetUser getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(TargetUser targetUser) {
        this.targetUser = targetUser;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public String getDateHuman() {
        return dateHuman;
    }

    public void setDateHuman(String dateHuman) {
        this.dateHuman = dateHuman;
    }

    public boolean isHasUnread() {
        return hasUnread;
    }

    public void setHasUnread(boolean hasUnread) {
        this.hasUnread = hasUnread;
    }

    public boolean isBlacklist() {
        return isBlacklist;
    }

    public void setBlacklist(boolean blacklist) {
        this.isBlacklist = blacklist;
    }

    public static class TargetUser {
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        @SerializedName("avatar")
        private String avatar;

        public int getId() {
            return id;
        }

        public void setId(int id) {
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
    }
}
