package com.lanzou.split.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class LanzouDownloadResponse {

    @SerializedName("zt")
    private int status;

    private String dom;

    private String url;

    private String info;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDom() {
        return dom;
    }

    public void setDom(String dom) {
        this.dom = dom;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @NonNull
    @Override
    public String toString() {
        return "LanzouDownloadResponse{" +
                "status=" + status +
                ", dom='" + dom + '\'' +
                ", url='" + url + '\'' +
                ", info='" + info + '\'' +
                '}';
    }
}
