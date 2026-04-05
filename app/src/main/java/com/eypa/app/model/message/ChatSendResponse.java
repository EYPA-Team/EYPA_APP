package com.eypa.app.model.message;

public class ChatSendResponse {
    private int code;
    private String msg;
    private ChatRecord data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public ChatRecord getData() {
        return data;
    }

    public void setData(ChatRecord data) {
        this.data = data;
    }
}
