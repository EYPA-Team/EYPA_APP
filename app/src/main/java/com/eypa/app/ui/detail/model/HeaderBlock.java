package com.eypa.app.ui.detail.model;

public class HeaderBlock implements ContentBlock {
    private final String text;
    private final int level; // 1=h1, 2=h2, etc. (用于决定字号大小)

    public HeaderBlock(String text, int level) {
        this.text = text;
        this.level = level;
    }

    public String getText() { return text; }
    public int getLevel() { return level; }
}