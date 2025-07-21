package com.lanzou.cloud.ui.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.R;
import com.lanzou.cloud.adapter.FileAdapter;
import com.lanzou.cloud.base.java.BaseActivity;
import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.databinding.ActivityRecycleFileBinding;
import com.lanzou.cloud.network.Repository;

import java.util.ArrayList;
import java.util.List;

public class RecycleFileActivity extends BaseActivity {

    private static final String TAG = "RecycleFileActivity";

    private final List<LanzouFile> files = new ArrayList<>();
    private final FileAdapter fileAdapter = new FileAdapter(files);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityRecycleFileBinding binding = ActivityRecycleFileBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.rvRecycle.setAdapter(fileAdapter);

        CharSequence[] options = new CharSequence[]{"恢复文件", "永久删除"};

        fileAdapter.setOnItemClickListener((position, view) -> {
            DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
                action(which, position);
            };

            new MaterialAlertDialogBuilder(this)
                    .setTitle("选择操作")
                    .setItems(options, onClickListener)
                    .setPositiveButton("关闭", null)
                    .show();
        });

        fileAdapter.setLongClickListener((position, v) -> {
            // ...
        });

        new Thread(() -> {
            files.addAll(Repository.getInstance().getRecycleFiles());
            runOnUiThread(new Runnable() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void run() {
                    fileAdapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.INVISIBLE);
                }
            });
        }).start();
    }

    private void action(int which, int position) {
        new Thread(() -> {
            LanzouFile lanzouFile = files.get(position);
            long fileId = lanzouFile.isFolder() ? lanzouFile.getFolderId() : lanzouFile.getFileId();
            if (Repository.getInstance().deleteRecycleFile(fileId, lanzouFile.isFolder(), which == 0)) {
                runOnUiThread(() -> {
                    if (which == 0) {
                        Toast.makeText(this, "文件已恢复到根目录", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "文件已永久删除", Toast.LENGTH_SHORT).show();
                    }
                    files.remove(position);
                    fileAdapter.notifyItemRemoved(position);
                });
            } else {
                // 删除失败
                Looper.prepare();
                Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recycle_bin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_all) {
            handleRecycleFiles("delete_all");
        } else if (item.getItemId() == R.id.restore_all) {
            handleRecycleFiles("restore_all");
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleRecycleFiles(String action) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("提示")
                .setMessage("是否执行操作，不可撤销")
                .setPositiveButton("执行", (dialog, which) -> {
                    Repository repository = Repository.getInstance();
                    new Thread(() -> {
                        if (repository.handleRecycleFiles(action)) {
                            runOnUiThread(new Runnable() {
                                @SuppressLint("NotifyDataSetChanged")
                                @Override
                                public void run() {
                                    files.clear();
                                    fileAdapter.notifyDataSetChanged();
                                }
                            });
                        } else {
                            Looper.prepare();
                            Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
