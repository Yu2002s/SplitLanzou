package com.lanzou.cloud.data;

public class LanzouFolder {

    private long folder_id;
    private String folder_name;

    public LanzouFolder() {
    }

    public LanzouFolder(long folder_id, String folder_name) {
        this.folder_id = folder_id;
        this.folder_name = folder_name;
    }

    public long getFolder_id() {
        return folder_id;
    }

    public void setFolder_id(long folder_id) {
        this.folder_id = folder_id;
    }

    public String getFolder_name() {
        return folder_name;
    }

    public void setFolder_name(String folder_name) {
        this.folder_name = folder_name;
    }
}
