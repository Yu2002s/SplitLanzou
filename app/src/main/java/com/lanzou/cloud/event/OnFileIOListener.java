package com.lanzou.cloud.event;

public interface OnFileIOListener {

    void onProgress(long current, long length, long byteCount);
}
