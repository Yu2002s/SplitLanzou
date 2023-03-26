package com.lanzou.split.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Comparator;

public class Download implements Comparable<Download>, Parcelable {

    /**
     * 正在读取文件信息
     */
    public static final int READ = -1;

    private String name;

    private String url;

    private String path;

    private String pwd;

    private long current;

    private long length;

    private int status;

    private int progress;

    private int speed;

    private Long time;

    /**
     * 上传文件json
     */
    private StringBuilder uploadJson;

    // 获取文件所有的上传信息
    private Upload upload;

    public Download() {

    }

    protected Download(Parcel in) {
        name = in.readString();
        url = in.readString();
        path = in.readString();
        pwd = in.readString();
        current = in.readLong();
        length = in.readLong();
        status = in.readInt();
        progress = in.readInt();
        speed = in.readInt();
        if (in.readByte() == 0) {
            time = null;
        } else {
            time = in.readLong();
        }
        upload = in.readParcelable(Upload.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(path);
        dest.writeString(pwd);
        dest.writeLong(current);
        dest.writeLong(length);
        dest.writeInt(status);
        dest.writeInt(progress);
        dest.writeInt(speed);
        if (time == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(time);
        }
        dest.writeParcelable(upload, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Download> CREATOR = new Creator<Download>() {
        @Override
        public Download createFromParcel(Parcel in) {
            return new Download(in);
        }

        @Override
        public Download[] newArray(int size) {
            return new Download[size];
        }
    };

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    @Nullable
    public String getPwd() {
        return pwd;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public Upload getUpload() {
        return upload;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getLength() {
        return length;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getSpeed() {
        return speed;
    }

    public void setUploadJson(StringBuilder uploadJson) {
        this.uploadJson = uploadJson;
    }

    public StringBuilder getUploadJson() {
        return uploadJson;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public void insert() {
        status = Upload.INSERT;
    }

    public void prepare() {
        status = Upload.PREPARE;
    }

    public void read() {
        status = READ;
    }

    public void progress() {
        status = Upload.PROGRESS;
    }

    public void error() {
        status = Upload.ERROR;
    }

    public void complete() {
        status = Upload.COMPLETE;
    }

    public void stop() {
        status = Upload.STOP;
    }

    public boolean isComplete() {
        return status == Upload.COMPLETE;
    }

    public boolean isDownload() {
        return status < Upload.ERROR;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusStr() {
        switch (status) {
            case Upload.INSERT:
                return "已加入队列";
            case Upload.PREPARE:
                return "准备中";
            case Upload.PROGRESS:
                return "下载中";
            case Upload.ERROR:
                return "下载失败";
            case Upload.COMPLETE:
                return "已下载";
            case READ:
                return "读取中";
            default:
                return "已停止";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Download download = (Download) o;

        return url.equals(download.url);
    }

    @Override
    public int compareTo(Download o) {
        return o.time.compareTo(time);
    }

    @NonNull
    @Override
    public String toString() {
        return "Download{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", pwd='" + pwd + '\'' +
                ", current=" + current +
                ", length=" + length +
                ", status=" + status +
                ", progress=" + progress +
                ", upload=" + upload +
                '}';
    }
}
