package com.eypa.app.model.message;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("type_text")
    private String typeText;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("create_time")
    private String createTime;

    @SerializedName("date_human")
    private String dateHuman;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("sender")
    private Sender sender;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeText() {
        return typeText;
    }

    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getDateHuman() {
        return dateHuman;
    }

    public void setDateHuman(String dateHuman) {
        this.dateHuman = dateHuman;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public static class Sender {
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
