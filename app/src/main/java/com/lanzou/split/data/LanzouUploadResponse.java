package com.lanzou.split.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LanzouUploadResponse {

    @SerializedName("zt")
    private int status;

    private String info;

    @SerializedName("text")
    private List<UploadInfo> uploadInfos;

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

    public List<UploadInfo> getUploadInfos() {
        return uploadInfos;
    }

    public void setUploadInfos(List<UploadInfo> uploadInfos) {
        this.uploadInfos = uploadInfos;
    }

    public static class UploadInfo {
        @SerializedName("f_id")
        private String fileId;
        private String name_all;
        private long id;
        private String size;

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getName_all() {
            return name_all;
        }

        public void setName_all(String name_all) {
            this.name_all = name_all;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "LanzouUploadResponse{" +
                "status=" + status +
                ", info='" + info + '\'' +
                ", uploadInfos=" + uploadInfos +
                '}';
    }
}
