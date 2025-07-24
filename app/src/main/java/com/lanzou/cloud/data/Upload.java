package com.lanzou.cloud.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Since;
import com.lanzou.cloud.event.OnUploadListener;
import com.lanzou.cloud.utils.FileJavaUtils;

import org.litepal.LitePal;
import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

import java.util.ArrayList;
import java.util.List;

public class Upload extends LitePalSupport implements Parcelable, Comparable<Upload> {

    public static final int INSERT = 0;
    public static final int PREPARE = 1;
    public static final int PROGRESS = 2;
    public static final int ERROR = 3;
    public static final int COMPLETE = 4;
    public static final int STOP = 5;

    private long id;

    private long download_id;

    /**
     * 当前上传区块
     */
    @Since(1.1)
    private int index;

    private String name;
    private long length;

    /**
     * 进度不参与序列化和反序列化
     */
    @Since(1.1)
    private int progress;

    @Since(1.1)
    private String path;

    @Since(1.1)
    private long current;

    @Since(1.1)
    private LanzouPage uploadPage;

    @Since(1.1)
    private int status;

    /**
     * 下载速度
     */
    @Since(1.1)
    private int speed;

    @Since(1.1)
    private int blockSize;

    @Since(1.1)
    private Long time;

    private long fileId;

    private List<SplitFile> files;

    private final String comment = "如需完整下载此文件，请下载app（https://github.com/Yu2002s/SplitLanzou）";

    @Column(ignore = true)
    private OnUploadListener listener;

    public Upload() {
    }

    public void update() {
        update(id);
    }

    public void copy(@NonNull Upload target) {
        this.index = target.index;
        this.name = target.name;
        this.length = target.length;
        this.progress = target.progress;
        this.path = target.path;
        this.current = target.current;
        this.uploadPage = target.uploadPage;
        this.status = target.status;
        this.speed = target.speed;
        this.blockSize = target.blockSize;
        this.time = target.time;
        this.files = target.files;
    }

    protected Upload(Parcel in) {
        id = in.readLong();
        index = in.readInt();
        name = in.readString();
        length = in.readLong();
        progress = in.readInt();
        path = in.readString();
        current = in.readLong();
        uploadPage = in.readParcelable(LanzouPage.class.getClassLoader());
        status = in.readInt();
        speed = in.readInt();
        blockSize = in.readInt();
        time = in.readLong();
        fileId = in.readLong();
        if (files == null) {
            files = new ArrayList<>();
        }
        in.readList(files, SplitFile.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(index);
        dest.writeString(name);
        dest.writeLong(length);
        dest.writeInt(progress);
        dest.writeString(path);
        dest.writeLong(current);
        dest.writeParcelable(uploadPage, flags);
        dest.writeInt(status);
        dest.writeInt(speed);
        dest.writeInt(blockSize);
        dest.writeLong(time == null ? 0 : time);
        dest.writeLong(fileId);
        dest.writeList(files);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Upload> CREATOR = new Creator<Upload>() {
        @Override
        public Upload createFromParcel(Parcel in) {
            return new Upload(in);
        }

        @Override
        public Upload[] newArray(int size) {
            return new Upload[size];
        }
    };

    public String getComment() {
        return comment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength() {
        return length;
    }

    public String getSize() {
        return FileJavaUtils.toSize(length);
    }

    public void setLength(long length) {
        this.length = length;
    }

    public List<SplitFile> getFiles() {
        return files;
    }

    public void setFiles(List<SplitFile> files) {
        this.files = files;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public LanzouPage getUploadPage() {
        return uploadPage;
    }

    public void setUploadPage(LanzouPage uploadPage) {
        this.uploadPage = uploadPage;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setListener(OnUploadListener listener) {
        this.listener = listener;
    }

    public OnUploadListener getListener() {
        return listener;
    }

    public void insert() {
        status = INSERT;
        if (listener != null)
            listener.onUpload(this);
    }

    public void prepare() {
        status = PREPARE;
        if (listener != null)
            listener.onUpload(this);
    }

    public void onProgress() {
        if (listener != null)
            listener.onUpload(this);
    }

    public void progress() {
        status = PROGRESS;
        if (listener != null)
            listener.onUpload(this);
    }

    public void error() {
        status = ERROR;
        if (listener != null)
            listener.onUpload(this);
    }

    public void complete() {
        status = COMPLETE;
        if (listener != null)
            listener.onUpload(this);
    }

    public void stop() {
        status = STOP;
        if (listener != null)
            listener.onUpload(this);
    }

    public boolean isInsert() {
        return status == INSERT;
    }

    public boolean isComplete() {
        return status == COMPLETE;
    }

    public boolean isUpload() {
        return status < ERROR;
    }

    public boolean isStop() {
        return status == STOP || status == ERROR;
    }

    public long getDownloadId() {
        return download_id;
    }

    public void setDownloadId(long downloadId) {
        this.download_id = downloadId;
    }

    @Override
    public int delete() {
        if (id != 0) {
            return super.delete();
        }
        return LitePal.deleteAll(Upload.class, "download_id = ?", String.valueOf(download_id));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Upload)) return false;

        Upload upload = (Upload) o;

        return getPath() != null ? getPath().equals(upload.getPath()) : upload.getPath() == null;
    }

    @Override
    public int compareTo(Upload o) {
        return o.time.compareTo(time);
    }

    public String getStatusStr() {
        switch (status) {
            case INSERT:
                return "已加入队列";
            case PREPARE:
                return "准备中";
            case PROGRESS:
                return "上传中";
            case ERROR:
                return "上传失败";
            case COMPLETE:
                return "已上传";
            default:
                return "已停止";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Upload{" +
                "id=" + id +
                ", download_id=" + download_id +
                ", index=" + index +
                ", name='" + name + '\'' +
                ", length=" + length +
                ", progress=" + progress +
                ", path='" + path + '\'' +
                ", current=" + current +
                ", uploadPage=" + uploadPage +
                ", status=" + status +
                ", speed=" + speed +
                ", blockSize=" + blockSize +
                ", time=" + time +
                ", fileId=" + fileId +
                ", files=" + files +
                ", comment='" + comment + '\'' +
                '}';
    }
}
