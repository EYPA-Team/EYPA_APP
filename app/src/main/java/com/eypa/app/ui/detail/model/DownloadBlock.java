package com.eypa.app.ui.detail.model;

public class DownloadBlock implements ContentBlock {
    private final String fileName;
    private final String fileType;
    private final String fileSize;
    private final String downloadUrl;

    public DownloadBlock(String fileName, String fileType, String fileSize, String downloadUrl) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.downloadUrl = downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}