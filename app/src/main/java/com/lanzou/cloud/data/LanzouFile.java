package com.lanzou.cloud.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class LanzouFile implements Parcelable {

    @SerializedName("id")
    private long fileId;

    @SerializedName("fol_id")
    private long folderId;

    private String time;

    @SerializedName("icon")
    private String extension;

    @SerializedName("downs")
    private int downloadCount;

    @SerializedName("folder_des")
    private String desc;

    private boolean isSelected = false;

    public LanzouFile() {}

    protected LanzouFile(Parcel in) {
        fileId = in.readLong();
        folderId = in.readLong();
        time = in.readString();
        extension = in.readString();
        downloadCount = in.readInt();
        size = in.readString();
        name = in.readString();
        name_all = in.readString();
        desc = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(fileId);
        dest.writeLong(folderId);
        dest.writeString(time);
        dest.writeString(extension);
        dest.writeInt(downloadCount);
        dest.writeString(size);
        dest.writeString(name);
        dest.writeString(name_all);
        dest.writeString(desc);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LanzouFile> CREATOR = new Creator<LanzouFile>() {
        @Override
        public LanzouFile createFromParcel(Parcel in) {
            return new LanzouFile(in);
        }

        @Override
        public LanzouFile[] newArray(int size) {
            return new LanzouFile[size];
        }
    };

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

    public String getFileName() {
        return isFolder() ? name : name_all;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LanzouFile that = (LanzouFile) o;

        if (isFolder() && folderId == that.folderId) return true;
        return fileId != 0 && fileId == that.fileId;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(fileId);
        result = 31 * result + Long.hashCode(folderId);
        return result;
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
