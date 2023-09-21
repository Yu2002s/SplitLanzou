package com.lanzou.cloud.data;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class LanzouUrl {

    @Nullable
    private String pwd;

    @SerializedName("f_id")
    private String fileId;

    @SerializedName("is_newd")
    private String host;

    @SerializedName("onof")
    private int hasPwd;

    @Nullable
    public String getPwd() {
        return pwd;
    }

    public void setPwd(@Nullable String pwd) {
        this.pwd = pwd;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getHasPwd() {
        return hasPwd;
    }

    public void setHasPwd(int hasPwd) {
        this.hasPwd = hasPwd;
    }
}
