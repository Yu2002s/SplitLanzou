package com.lanzou.split.ui.file.imple;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.data.LanzouPage;
import com.lanzou.split.data.LanzouSimpleResponse;
import com.lanzou.split.data.LanzouUrl;
import com.lanzou.split.event.FileActionListener;
import com.lanzou.split.network.Repository;
import com.lanzou.split.ui.file.AbstractFileAction;
import com.lanzou.split.ui.folder.FolderSelectorActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileActionImpl extends AbstractFileAction {

    private final List<LanzouPage> lanzouPages = new LinkedList<>();
    private final List<LanzouFile> lanzouFiles = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private LanzouPage currentPage = new LanzouPage();

    private AppCompatActivity mActivity;

    private FileActionListener fileActionListener;

    private ClipboardManager clipboardManager;

    @Override
    public List<LanzouFile> getSource() {
        return lanzouFiles;
    }

    @Override
    public List<LanzouPage> getLanzouPages() {
        return lanzouPages;
    }

    @Override
    public LanzouPage getCurrentPage() {
        return currentPage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bindView(RecyclerView rv, FileActionListener fileActionListener) {
        mActivity = (AppCompatActivity) rv.getContext();
        clipboardManager = (ClipboardManager) mActivity
                .getSystemService(Context.CLIPBOARD_SERVICE);
        adapter = rv.getAdapter();
        this.fileActionListener = fileActionListener;
        OnBackPressedDispatcher backPressedDispatcher = mActivity.getOnBackPressedDispatcher();
        backPressedDispatcher.addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        });
        StaggeredGridLayoutManager staggeredGridLayoutManager =
                (StaggeredGridLayoutManager) rv.getLayoutManager();
        assert staggeredGridLayoutManager != null;
        int[] spans = new int[staggeredGridLayoutManager.getSpanCount()];
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    staggeredGridLayoutManager.findLastVisibleItemPositions(spans);
                    int lastVisibleItemPosition = findMax(spans);
                    if (currentPage.isCompleted() && currentPage.isNotNull()
                            && lastVisibleItemPosition >= adapter.getItemCount() - 6) {
                        // loadMore
                        currentPage.setCompleted(false);
                        loadMoreFiles();
                    }
                }
            }
        });
    }

    private int findMax(int[] spans) {
        if (spans.length == 0) return 0;
        int max = spans[0];
        for (int span : spans) {
            if (max < span) {
                max = span;
            }
        }
        return max;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void navigateTo(int position) {
        int size = lanzouPages.size() - 1;
        if (position == size) {
            return;
        }
        if (size - position == 1) {
            onBackPressed();
            return;
        }
        for (int i = size; i > position; i--) {
            lanzouPages.remove(i);
        }
        currentPage = lanzouPages.get(position);
        lanzouFiles.clear();
        lanzouFiles.addAll(lanzouPages.get(position).getFiles());
        adapter.notifyDataSetChanged();
        fileActionListener.onPageChange();
    }

    @Override
    public void deleteItem(int position) {
        if (position == -1) return;
        lanzouFiles.remove(position);
        currentPage.getFiles().remove(position);
    }

    @Override
    public void refresh() {
        lanzouPages.remove(currentPage);
        currentPage.setPage(1);
        getFiles(currentPage.getFolderId(), currentPage.getName());
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void getFiles(long folderId, String folderName) {
        fileActionListener.onPreLoadFile();
        initPage(folderId, folderName);
        if (lanzouPages.size() != 1) {
            fileActionListener.onPageChange();
        }
        if (!lanzouFiles.isEmpty() && currentPage.getPage() == 1) {
             lanzouFiles.clear();
            adapter.notifyDataSetChanged();
        }
        new Thread(() -> {
            List<LanzouFile> files = Repository.getInstance().getFiles(folderId, 1);
            if (files != null && !files.isEmpty()) {
                if (files.size() < 18) {
                    currentPage.setNull(true);
                }
                lanzouFiles.addAll(files);
                currentPage.addFiles(lanzouFiles);
            } else {
                currentPage.setNull(true);
            }
            currentPage.setCompleted(true);
            mActivity.runOnUiThread(() -> {
                fileActionListener.onFileLoaded();
                adapter.notifyDataSetChanged();
                if (currentPage.isNotNull()) {
                    loadMoreFiles();
                }
            });
        }).start();
    }

    private void initPage(long folderId, String folderName) {
        currentPage = new LanzouPage();
        currentPage.setFolderId(folderId);
        currentPage.setName(folderName);
        lanzouPages.add(currentPage);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBackPressed() {
        if (lanzouPages.size() == 1) {
            // 默认返回
            mActivity.finish();
            return;
        }

        int position = lanzouPages.size() - 2;
        LanzouPage lanzouPage = lanzouPages.get(position);
        currentPage = lanzouPage;
        lanzouFiles.clear();
        if (lanzouPage.getFiles() != null) {
            lanzouFiles.addAll(lanzouPage.getFiles());
        }
        adapter.notifyDataSetChanged();
        lanzouPages.remove(position + 1);
        // mActivity.setTitle(currentPage.getName());
        fileActionListener.onPageChange();
    }

    @Override
    public void loadMoreFiles() {
        currentPage.nextPage();
        new Thread(() -> {
            List<LanzouFile> files = Repository.getInstance()
                    .getFiles(currentPage.getFolderId(), currentPage.getPage());
            if (files == null || files.isEmpty()) {
                currentPage.setNull(true);
                return;
            }
            if (files.size() < 18) {
                currentPage.setNull(true);
            }
            currentPage.setCompleted(true);
            lanzouFiles.addAll(files);
            int size = lanzouFiles.size();
            int start = size - files.size();
            mActivity.runOnUiThread(() -> adapter.notifyItemRangeInserted(start, size));
        }).start();
    }

    @Override
    public void deleteFile(int position) {
        LanzouFile lanzouFile = lanzouFiles.get(position);
        new Thread(() -> {
            LanzouSimpleResponse response = Repository.getInstance().deleteFile(lanzouFile);
            mActivity.runOnUiThread(() -> {
                if (response != null) {
                    if (response.getStatus() == 1) {
                        deleteItem(position);
                        adapter.notifyItemRemoved(position);
                    }
                    Toast.makeText(mActivity, response.getInfo(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public void shareFile(int position) {
        LanzouFile lanzouFile = lanzouFiles.get(position);
        new Thread(() -> {
            LanzouUrl lanzouUrl = Repository.getInstance().getLanzouUrl(lanzouFile.getFileId());
            if (lanzouUrl == null) {
                // Toast.makeText(mActivity, "分享文件出错", Toast.LENGTH_SHORT).show();
                return;
            }
            mActivity.runOnUiThread(() -> {
                String str = lanzouUrl.getHost() + "/tp/" + lanzouUrl.getFileId();
                if (lanzouUrl.getHasPwd() == 1) {
                    str += "\n密码: " + lanzouUrl.getPwd();
                }
                clipboardManager.setPrimaryClip(ClipData.newPlainText("url", str));
                Toast.makeText(mActivity, "分享链接已复制", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    public void moveFile(ActivityResultLauncher<Intent> launcher, LanzouFile lanzouFile) {
        FolderSelectorActivity.moveFile(mActivity, launcher, lanzouFile);
    }
}
