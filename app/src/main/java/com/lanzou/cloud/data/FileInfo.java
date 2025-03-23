package com.lanzou.cloud.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FileInfo implements Comparable<FileInfo> {

    private Long id;

    private String name;

    private String uri;

    private Long length;

    private Long time;

    private String pkgName;

    @Nullable
    private String extension;

    private String fileDesc;

    public String getFileDesc() {
        return fileDesc;
    }

    public void setFileDesc(String fileDesc) {
        this.fileDesc = fileDesc;
    }

    @Nullable
    public String getExtension() {
        return extension;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setExtension(@Nullable String extension) {
        this.extension = extension;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    private boolean isSelected = false;

    public FileInfo(String name, String uri, Long length) {
        this.name = name;
        this.uri = uri;
        this.length = length;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfo fileInfo = (FileInfo) o;

        if (id != null && id.equals(fileInfo.id)) return true;
        return uri.equals(fileInfo.uri);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + uri.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "FileInfo{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                ", length=" + length +
                ", isSelected=" + isSelected +
                '}';
    }

    @Override
    public int compareTo(FileInfo o) {
        return o.getTime().compareTo(time);
    }
}
