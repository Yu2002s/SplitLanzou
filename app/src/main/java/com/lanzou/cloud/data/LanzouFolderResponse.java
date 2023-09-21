package com.lanzou.cloud.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LanzouFolderResponse {

    @SerializedName("zt")
    private int status;

    @SerializedName("info")
    private List<LanzouFolder> folders;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<LanzouFolder> getFolders() {
        return folders;
    }

    public void setFolders(List<LanzouFolder> folders) {
        this.folders = folders;
    }
}
