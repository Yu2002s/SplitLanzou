package com.lanzou.split.ui.file;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.data.LanzouPage;
import com.lanzou.split.event.FileActionListener;

import java.util.List;

public abstract class AbstractFileAction {

    public abstract void refresh();

    public abstract void getFiles(long folderId, String folderName);

    public abstract void onBackPressed();

    protected abstract void loadMoreFiles();

    public abstract List<LanzouFile> getSource();

    public abstract List<LanzouPage> getLanzouPages();

    public abstract LanzouPage getCurrentPage();

    public abstract void bindView(RecyclerView rv, FileActionListener fileActionListener);

    protected void getFiles() {
        getFiles(-1, "根目录");
    }

    public void navigateTo(int position) {}

    public void deleteItem(int position) {}

    public void deleteFile(int position) {}

    public void deleteFiles(Callback callback) {}

    public void shareFile(int position) {}

    public void moveFile(ActivityResultLauncher<Intent> launcher, LanzouFile lanzouFile) {}

    public void moveFiles(ActivityResultLauncher<Intent> launcher, List<LanzouFile> lanzouFiles) {}


    public interface Callback {
        void onCompleted();
    }
}
