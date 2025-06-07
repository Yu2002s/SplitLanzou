package com.lanzou.cloud.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Since;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

public class SplitFile extends LitePalSupport implements Parcelable {

    /**
     * 唯一 id
     */
    private long id;

    /**
     * 文件批次
     */
    private int index;
    /**
     * 起始位置
     */
    private long start;
    /**
     * 结束位置
     */
    private long end;

    /**
     * 文件大小
     */
    private long length;

    /**
     * 文件唯一ID
     */
    private long fileId;

    /**
     * 文件唯一分享地址 这个地址进行AES加密
     */
    private String url;

    /**
     * 文件下载密码
     */
    @Nullable
    private String pwd;

    /**
     * 控制单个文件字节读取开始位置
     */
    @Since(1.1)
    private long byteStart;

    public SplitFile() {
    }

    public void update() {
        if (id == 0) {
            updateAll("fileId = ?", String.valueOf(fileId));
        } else {
            update(id);
        }
    }

    @Override
    public int delete() {
        if (id != 0) {
            return super.delete();
        }
        return LitePal.deleteAll(SplitFile.class, "fileId = ?", String.valueOf(fileId));
    }

    protected SplitFile(Parcel in) {
        index = in.readInt();
        start = in.readLong();
        end = in.readLong();
        length = in.readLong();
        fileId = in.readLong();
        url = in.readString();
        pwd = in.readString();
        byteStart = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(index);
        dest.writeLong(start);
        dest.writeLong(end);
        dest.writeLong(length);
        dest.writeLong(fileId);
        dest.writeString(url);
        dest.writeString(pwd);
        dest.writeLong(byteStart);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SplitFile> CREATOR = new Creator<SplitFile>() {
        @Override
        public SplitFile createFromParcel(Parcel in) {
            return new SplitFile(in);
        }

        @Override
        public SplitFile[] newArray(int size) {
            return new SplitFile[size];
        }
    };

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Nullable
    public String getPwd() {
        return pwd;
    }

    public void setPwd(@Nullable String pwd) {
        this.pwd = pwd;
    }

    public long getByteStart() {
        return byteStart;
    }

    public void setByteStart(long byteStart) {
        this.byteStart = byteStart;
    }

    public boolean isComplete() {
        return byteStart == length;
    }

    @NonNull
    @Override
    public String toString() {
        return "SplitFile{" +
                "index=" + index +
                ", start=" + start +
                ", end=" + end +
                ", length=" + length +
                ", fileId=" + fileId +
                ", url='" + url + '\'' +
                ", pwd='" + pwd + '\'' +
                '}';
    }
}
