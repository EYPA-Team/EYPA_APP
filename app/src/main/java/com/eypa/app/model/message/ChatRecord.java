package com.eypa.app.model.message;

import com.google.gson.annotations.SerializedName;

public class ChatRecord {
    @SerializedName("id")
    private String id;

    @SerializedName("is_me")
    private boolean isMe;

    @SerializedName("content")
    private String content;

    @SerializedName("create_time")
    private String createTime;

    @SerializedName("is_read")
    private boolean isRead;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
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

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
