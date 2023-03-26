package com.lanzou.split.event;

public interface OnFileIOListener {

    void onProgress(long current, long length, long byteCount);
}
