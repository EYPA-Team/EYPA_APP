package com.eypa.app.model.bbs;

import com.google.gson.annotations.SerializedName;

public class BBSPostDetailResponse {
    @SerializedName("code")
    private int code;

    @SerializedName("msg")
    private String msg;

    @SerializedName("data")
    private BBSPost data;

    public boolean isSuccess() {
        return code == 200;
    }

    public BBSPost getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }
}
