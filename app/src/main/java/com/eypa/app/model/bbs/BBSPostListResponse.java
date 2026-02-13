package com.eypa.app.model.bbs;

import java.util.List;

public class BBSPostListResponse {
    private int code;
    private String msg;
    private List<BBSPost> data;

    public boolean isSuccess() {
        return code == 200;
    }

    public String getMsg() {
        return msg;
    }

    public List<BBSPost> getData() {
        return data;
    }
}
