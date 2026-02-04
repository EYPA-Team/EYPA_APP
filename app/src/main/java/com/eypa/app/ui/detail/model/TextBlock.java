package com.eypa.app.ui.detail.model;

import android.text.Spanned;

public class TextBlock implements ContentBlock {
    private final Spanned content;

    public TextBlock(Spanned content) {
        this.content = content;
    }

    public Spanned getContent() {
        return content;
    }
}