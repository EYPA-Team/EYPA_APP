package com.eypa.app.model.message;

public class MessageReadRequest {
    private String token;
    private String msg_id;

    public MessageReadRequest(String token, String msg_id) {
        this.token = token;
        this.msg_id = msg_id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMsgId() {
        return msg_id;
    }

    public void setMsgId(String msg_id) {
        this.msg_id = msg_id;
    }
}
