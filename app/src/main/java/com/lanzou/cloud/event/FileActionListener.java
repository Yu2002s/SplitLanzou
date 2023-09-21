package com.lanzou.cloud.event;

public interface FileActionListener {

    void onPreLoadFile();

    void onFileLoaded();

    void onPageChange();

}
