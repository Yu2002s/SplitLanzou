package com.lanzou.cloud.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.litepal.crud.LitePalSupport;

import java.util.ArrayList;
import java.util.List;

public class LanzouPage extends LitePalSupport implements Parcelable {

    private long folderId;

    private String name;

    private int page = 1;

    private boolean isCompleted = false;
    private boolean isNull = false;

    public LanzouPage() {
    }

    protected LanzouPage(Parcel in) {
        folderId = in.readLong();
        name = in.readString();
        page = in.readInt();
        isCompleted = in.readByte() != 0;
        isNull = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(folderId);
        dest.writeString(name);
        dest.writeInt(page);
        dest.writeByte((byte) (isCompleted ? 1 : 0));
        dest.writeByte((byte) (isNull ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LanzouPage> CREATOR = new Creator<LanzouPage>() {
        @Override
        public LanzouPage createFromParcel(Parcel in) {
            return new LanzouPage(in);
        }

        @Override
        public LanzouPage[] newArray(int size) {
            return new LanzouPage[size];
        }
    };

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isNull() {
        return isNull;
    }

    public boolean isNotNull() {
        return !isNull();
    }

    public void setNull(boolean aNull) {
        isNull = aNull;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void nextPage() {
        page++;
    }

    private final List<LanzouFile> files = new ArrayList<>();

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LanzouFile> getFiles() {
        return files;
    }

    public void addFiles(List<LanzouFile> files) {
        this.files.addAll(files);
    }
}
