package com.lanzou.cloud.data;

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
}
