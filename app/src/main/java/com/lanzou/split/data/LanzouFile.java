package com.lanzou.split.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Since;

public class LanzouFile {

    @SerializedName("id")
    private long fileId;

    @SerializedName("fol_id")
    private long folderId;

    private String time;

    @SerializedName("icon")
    private String extension;

    @SerializedName("downs")
    private int downloadCount;

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    private String size;

    public long getFolderId() {
        return folderId;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public boolean isFolder() {
        return folderId != 0;
    }

    private String name;
    private String name_all;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName_all() {
        return name_all;
    }

    public void setName_all(String name_all) {
        this.name_all = name_all;
    }

    @NonNull
    @Override
    public String toString() {
        return "LanzouFile{" +
                "fileId=" + fileId +
                ", folderId=" + folderId +
                ", name='" + name + '\'' +
                ", name_all='" + name_all + '\'' +
                '}';
    }
}
