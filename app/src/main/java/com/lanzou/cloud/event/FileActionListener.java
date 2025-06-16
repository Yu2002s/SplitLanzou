package com.lanzou.cloud.event;

import com.lanzou.cloud.data.LanzouFile;

public interface FileActionListener {

    void onPreLoadFile();

    void onFileLoaded();

    void onPageChange();

    void onMoveFile(LanzouFile lanzouFile, long id);
}
