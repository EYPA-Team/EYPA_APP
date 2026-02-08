package com.eypa.app.model.user;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AuthorFansResponse {
    @SerializedName("code")
    private int code;
    @SerializedName("msg")
    private String msg;
    @SerializedName("data")
    private List<FanItem> data;
    @SerializedName("pagination")
    private Pagination pagination;

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

    public List<FanItem> getData() {
        return data;
    }

    public void setData(List<FanItem> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Pagination {
        @SerializedName("page")
        private int page;
        @SerializedName("has_next")
        private boolean hasNext;

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public void setHasNext(boolean hasNext) {
            this.hasNext = hasNext;
        }
    }
}
