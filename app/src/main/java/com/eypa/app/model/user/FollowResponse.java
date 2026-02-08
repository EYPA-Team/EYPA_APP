package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;

public class FollowResponse {
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
        @SerializedName("target_id")
        private int targetId;

        @SerializedName("is_following")
        private boolean isFollowing;

        @SerializedName("fans_count")
        private int fansCount;

        public int getTargetId() {
            return targetId;
        }

        public boolean isFollowing() {
            return isFollowing;
        }

        public int getFansCount() {
            return fansCount;
        }
    }
}
