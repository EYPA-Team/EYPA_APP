package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

public class UpdateInfo {
    @SerializedName("version_code")
    private int versionCode;

    @SerializedName("version_name")
    private String versionName;

    @SerializedName("download_url")
    private String downloadUrl;

    @SerializedName("update_log")
    private String updateLog;

    @SerializedName("force_update")
    private boolean forceUpdate;

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getUpdateLog() {
        return updateLog;
    }

    public void setUpdateLog(String updateLog) {
        this.updateLog = updateLog;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }
}
