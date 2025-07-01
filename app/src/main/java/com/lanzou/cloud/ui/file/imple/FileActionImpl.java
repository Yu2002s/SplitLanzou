package com.lanzou.cloud.ui.file.imple;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.adapter.FileAdapter;
import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.data.LanzouPage;
import com.lanzou.cloud.data.LanzouSimpleResponse;
import com.lanzou.cloud.data.LanzouUrl;
import com.lanzou.cloud.databinding.DialogCreateFolderBinding;
import com.lanzou.cloud.event.FileActionListener;
import com.lanzou.cloud.network.Repository;
import com.lanzou.cloud.ui.file.FileAction;
import com.lanzou.cloud.ui.folder.FolderSelectorActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileActionImpl implements FileAction {

    private final List<LanzouPage> lanzouPages = new LinkedList<>();
    private final List<LanzouFile> lanzouFiles = new ArrayList<>();
    private FileAdapter adapter;
    private LanzouPage currentPage = new LanzouPage();

    private AppCompatActivity mActivity;

    private Fragment mFragment;

    private RecyclerView mRecyclerView;

    private FileActionListener fileActionListener;

    private ClipboardManager clipboardManager;

    private ActivityResultLauncher<Intent> moveActivityResultLauncher;

    private final LifecycleObserver mLifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            mActivity.getLifecycle().removeObserver(this);
            lanzouFiles.clear();
            lanzouPages.clear();
            moveActivityResultLauncher.unregister();
        }

        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {
            ActivityResultCallback<ActivityResult> callback = result -> {
                if (result.getData() != null) {
                    // lanzouFile 为需要移动的文件对象
                    new Thread(() -> {
                        LanzouFile lanzouFile = result.getData().getParcelableExtra("lanzouFile");
                        // id 为移动到目标文件夹的 id
                        long id = result.getData().getLongExtra("id", -1);
                        if (lanzouFile == null) {
                            handleMoveFiles(id);
                        } else {
                            handleMoveFile(lanzouFile, id);
                        }
                        fileActionListener.onMoveFile(lanzouFile, id);
                    }).start();
                }
            };
            moveActivityResultLauncher = mFragment.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), callback);
        }
    };

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

    @Override
    public void observable(Fragment context) {
        mFragment = context;
        mActivity = (AppCompatActivity) context.requireActivity();
        Lifecycle lifecycle = mActivity.getLifecycle();
        lifecycle.removeObserver(mLifecycleObserver);
        lifecycle.addObserver(mLifecycleObserver);
        clipboardManager = (ClipboardManager) mActivity
                .getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public void bindView(RecyclerView rv, FileActionListener fileActionListener) {
        if (mActivity == null) {
            throw new NullPointerException("请先调用 observable 方法");
        }
        adapter = (FileAdapter) rv.getAdapter();
        this.fileActionListener = fileActionListener;

        addBackCallback();

        initView(rv);
    }

    private void addBackCallback() {
        OnBackPressedDispatcher backPressedDispatcher = mActivity.getOnBackPressedDispatcher();
        backPressedDispatcher.addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        });
    }

    private void initView(RecyclerView rv) {
        mRecyclerView = rv;
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
    public boolean navigateTo(int position) {
        int size = lanzouPages.size() - 1;
        if (position == size) {
            // 导航到当前页面
            return false;
        }
        if (size - position == 1) {
            // 导航到上一页
            onBackPressed();
            return true;
        }
        for (int i = size; i > position; i--) {
            lanzouPages.remove(i);
        }
        currentPage = lanzouPages.get(position);
        lanzouFiles.clear();
        lanzouFiles.addAll(lanzouPages.get(position).getFiles());
        adapter.notifyDataSetChanged();
        fileActionListener.onPageChange();
        return true;
    }

    @Override
    public void deleteItem(int position) {
        if (position == -1) return;
        lanzouFiles.remove(position);
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
                currentPage.addFiles(files);
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
            lanzouFiles.addAll(currentPage.getFiles());
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
            currentPage.addFiles(files);
            lanzouFiles.addAll(files);
            int size = lanzouFiles.size();
            int start = size - files.size();
            mActivity.runOnUiThread(() -> adapter.notifyItemRangeInserted(start, size));
        }).start();
    }

    @Override
    public void createFolder() {
        LayoutInflater layoutInflater = mActivity.getLayoutInflater();
        DialogCreateFolderBinding folderBinding = DialogCreateFolderBinding.inflate(layoutInflater);
        Thread thread = new Thread(() -> {
            String name = folderBinding.editName.getText().toString();
            String desc = folderBinding.editDesc.getText().toString();
            LanzouPage currentPage = getCurrentPage();
            Long id = Repository.getInstance()
                    .createFolder(currentPage.getFolderId(), name, desc);
            mActivity.runOnUiThread(() -> {
                if (id != null) {
                    LanzouFile lanzouFile = new LanzouFile();
                    lanzouFile.setName(name);
                    lanzouFile.setFolderId(id);
                    lanzouFiles.add(0, lanzouFile);
                    adapter.notifyItemInserted(0);
                    mRecyclerView.scrollToPosition(0);
                    Toast.makeText(mActivity, "文件夹已新建", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, "新建文件夹失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle("新建文件夹")
                .setView(folderBinding.getRoot())
                .setPositiveButton("新建", (dialog, which) -> {
                    thread.start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteFile(LanzouFile lanzouFile, int position) {
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
    }

    @Override
    public void deleteFile(int position) {
        LanzouFile lanzouFile = lanzouFiles.get(position);
        new Thread(() -> {
            deleteFile(lanzouFile, position);
        }).start();
    }

    @Override
    public void deleteFiles(Callback callback) {
        new Thread(() -> {
            for (int i = lanzouFiles.size() - 1; i >= 0; i--) {
                LanzouFile lanzouFile = lanzouFiles.get(i);
                if (lanzouFile.isSelected()) {
                    deleteFile(lanzouFile, i);
                }
            }
            mActivity.runOnUiThread(callback::onCompleted);
        }).start();
    }

    @Override
    public void shareFile(int position) {
        LanzouFile lanzouFile = lanzouFiles.get(position);
        CharSequence[] items = new CharSequence[]{"自定义分享(支持分享100M+文件)", "普通分享(原始分享地址)"};
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle("选择分享")
                .setSingleChoiceItems(items, -1, (dialog, which) -> new Thread(() -> {
                    LanzouUrl lanzouUrl = Repository.getInstance().getLanzouUrl(lanzouFile.getFileId());
                    if (lanzouUrl == null) {
                        Looper.prepare();
                        Toast.makeText(mActivity, "分享文件出错", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                        return;
                    }
                    mActivity.runOnUiThread(() -> {
                        String str;
                        if (which == 0) {
                            str = LanzouApplication.SHARE_URL + "?url=" + lanzouUrl.getHost()
                                    + "/" + lanzouUrl.getFileId();
                            if (lanzouUrl.getHasPwd() == 1) {
                                str += "&pwd=" + lanzouUrl.getPwd();
                            }
                        } else {
                            str = lanzouUrl.getHost() + "/tp/" + lanzouUrl.getFileId();
                            if (lanzouUrl.getHasPwd() == 1) {
                                str += "\n密码: " + lanzouUrl.getPwd();
                            }
                        }
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("url", str));
                        Toast.makeText(mActivity, "分享链接已复制", Toast.LENGTH_SHORT).show();
                    });
                }).start())
                .setPositiveButton("关闭", null)
                .show();
    }

    @Override
    public void moveFile(LanzouFile lanzouFile) {
        FolderSelectorActivity.moveFile(mActivity, moveActivityResultLauncher, lanzouFile);
    }

    private void handleMoveFile(LanzouFile lanzouFile, long id) {
        LanzouSimpleResponse response = Repository.getInstance().moveFile(lanzouFile.getFileId(), id);
        if (response.getStatus() == 1) {
            mActivity.runOnUiThread(() -> {
                int position = lanzouFiles.indexOf(lanzouFile);
                adapter.deleteItem(position);
                getCurrentPage().getFiles().remove(position);
            });
        } else {
            Looper.prepare();
            Toast.makeText(mActivity, "移动文件失败", Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }

    private void handleMoveFiles(long id) {
        for (int i = lanzouFiles.size() - 1; i >= 0; i--) {
            LanzouFile file = lanzouFiles.get(i);
            if (file.isSelected() && !file.isFolder()) {
                LanzouSimpleResponse response = Repository.getInstance().moveFile(file.getFileId(), id);
                final int position = i;
                mActivity.runOnUiThread(() -> {
                    if (response.getStatus() == 1) {
                        adapter.deleteItem(position);
                        getCurrentPage().getFiles().remove(position);
                    }
                });
            }
        }
    }

    @Override
    public void moveFiles() {
        FolderSelectorActivity.moveFiles(mActivity, moveActivityResultLauncher);
    }
}
