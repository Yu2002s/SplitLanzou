package com.lanzou.split.data;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Since;

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
}
