package com.lanzou.cloud.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class LanzouUrlResponse {

    @SerializedName("zt")
    private int status;

    private LanzouUrl info;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LanzouUrl getInfo() {
        return info;
    }

    public void setInfo(LanzouUrl info) {
        this.info = info;
    }

    @NonNull
    @Override
    public String toString() {
        return "LanzouUrlResponse{" +
                "status=" + status +
                ", info=" + info +
                '}';
    }
}
