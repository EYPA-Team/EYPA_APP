package com.eypa.app.ui.detail.model;

import android.text.Spanned;

public class QuoteBlock implements ContentBlock {
    private final Spanned content;

    public QuoteBlock(Spanned content) {
        this.content = content;
    }

    public Spanned getContent() { return content; }
}