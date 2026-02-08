package com.eypa.app.ui.detail.model;

import com.eypa.app.model.Comment;

public class CommentBlock implements ContentBlock {
    private final Comment comment;
    private int depth;
    private boolean isExpanded = false;

    public CommentBlock(Comment comment, int depth) {
        this.comment = comment;
        this.depth = depth;
    }

    public Comment getComment() {
        return comment;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}