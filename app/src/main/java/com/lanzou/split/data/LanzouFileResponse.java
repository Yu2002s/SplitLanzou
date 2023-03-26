package com.lanzou.split.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LanzouFileResponse {

    @SerializedName("zt")
    private Integer status;
    @SerializedName("text")
    private List<LanzouFile> files;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<LanzouFile> getFiles() {
        return files;
    }

    public void setFiles(List<LanzouFile> files) {
        this.files = files;
    }
}
