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
        @SerializedName("per_page")
        private int perPage;
        @SerializedName("total_count")
        private int totalCount;
        @SerializedName("total_pages")
        private int totalPages;
        @SerializedName("has_next")
        private boolean hasNext;

        public int getPage() {
            return page;
        }

        public int getPerPage() {
            return perPage;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean isHasNext() {
            return hasNext;
        }
    }
}
