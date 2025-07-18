package com.lanzou.cloud.data;

import com.google.gson.annotations.SerializedName;

public class LanzouSimpleResponse {

    @SerializedName("zt")
    private int status;

    private String info;

    private String text;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "LanzouSimpleResponse{" +
                "status=" + status +
                ", info='" + info + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
