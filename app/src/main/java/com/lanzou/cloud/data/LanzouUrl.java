package com.lanzou.cloud.data;

import androidx.annotation.NonNull;
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

    @SerializedName("new_url")
    private String url;

    private String des;

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

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

    @NonNull
    @Override
    public String toString() {
        return "LanzouUrl{" +
                "pwd='" + pwd + '\'' +
                ", fileId='" + fileId + '\'' +
                ", host='" + host + '\'' +
                ", hasPwd=" + hasPwd +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
