package com.lanzou.split.data;

import com.lanzou.split.service.UploadService;

import java.util.concurrent.Future;

public class UploadTask {

    private Upload upload;
    private Future<?> task;

    public UploadTask(Upload upload, Future<?> task) {
        this.upload = upload;
        this.task = task;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public Future<?> getTask() {
        return task;
    }

    public void setTask(Future<?> task) {
        this.task = task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Upload) {
            return upload.equals(o);
        }
        return false;
    }

}
