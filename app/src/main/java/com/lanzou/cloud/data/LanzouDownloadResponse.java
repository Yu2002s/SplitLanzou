package com.lanzou.cloud.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import kotlinx.serialization.Serializable;

@Serializable
public class LanzouDownloadResponse {

    @SerializedName("zt")
    private int status;

    private String dom;

    private String url;

    private String inf;

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

    public String getInf() {
        return inf;
    }

    public void setInf(String inf) {
        this.inf = inf;
    }

    @NonNull
    @Override
    public String toString() {
        return "LanzouDownloadResponse{" +
                "status=" + status +
                ", dom='" + dom + '\'' +
                ", url='" + url + '\'' +
                ", inf='" + inf + '\'' +
                '}';
    }
}
