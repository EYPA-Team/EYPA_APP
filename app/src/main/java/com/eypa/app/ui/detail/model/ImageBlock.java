package com.eypa.app.ui.detail.model;

public class ImageBlock implements ContentBlock {
    private final String imageUrl;

    public ImageBlock(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}