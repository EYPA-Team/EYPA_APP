package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CommentsResponse {
    private Object code;
    private String msg;
    private List<Comment> data;
    private Pagination pagination;

    public boolean isSuccess() {
        if (code instanceof Integer) {
            return (Integer) code == 200;
        } else if (code instanceof Double) {
            return ((Double) code).intValue() == 200;
        }
        return false;
    }

    public String getMsg() {
        return msg;
    }

    public List<Comment> getData() {
        return data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public static class Pagination {
        private int page;
        @SerializedName("has_next")
        private boolean hasNext;

        public int getPage() {
            return page;
        }

        public boolean isHasNext() {
            return hasNext;
        }
    }
}
