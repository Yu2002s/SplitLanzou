package com.lanzou.cloud.ui.selector;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.lanzou.cloud.R;
import com.lanzou.cloud.adapter.FileSelectorAdapter;
import com.lanzou.cloud.base.BaseActivity;
import com.lanzou.cloud.data.FileInfo;
import com.lanzou.cloud.databinding.ActivityPhoneFileBinding;
import com.lanzou.cloud.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 手机文件选择器
 */
public class PhoneFileActivity extends BaseActivity {

    private ActivityPhoneFileBinding binding;

    private final List<FileInfo> fileInfos = new ArrayList<>();

    private final List<FileInfo> selectedFileInfos = new ArrayList<>();

    private final List<String> pathList = new ArrayList<>();

    private final FileSelectorAdapter fileSelectorAdapter = new FileSelectorAdapter(fileInfos);

    private static final String ROOT = Environment.getExternalStorageDirectory().getPath();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPhoneFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MaterialToolbar toolbar = binding.header.toolBar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        fileSelectorAdapter.setSelectButtonClickable(true);

        RecyclerView recyclerView = binding.fileRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fileSelectorAdapter);

        pathList.add(ROOT);
        getFiles(ROOT);

        binding.refreshLayout.setOnRefreshListener(this::getFiles);

        fileSelectorAdapter.setOnItemClickListener((position, view) -> {
            FileInfo fileInfo = fileInfos.get(position);
            if (fileInfo.getExtension() != null) {
                return;
            }
            pathList.add(fileInfo.getUri());
            getFiles(fileInfo.getUri());
        });

        fileSelectorAdapter.setOnSelectItemClickListener((position, view) -> {
            FileInfo fileInfo = fileInfos.get(position);
            if (fileInfo.isSelected()) {
                selectedFileInfos.add(fileInfo);
            } else {
                selectedFileInfos.remove(fileInfo);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (pathList.size() == 1) {
            super.onBackPressed();
            return;
        }
        String beforePath = pathList.get(pathList.size() - 2);
        pathList.remove(pathList.size() - 1);
        getFiles(beforePath);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_selector, menu);
        MenuItem item = menu.findItem(R.id.upload_file);
        Button upload = item.getActionView().findViewById(R.id.btn_upload);
        upload.setOnClickListener(v -> {
            ArrayList<CharSequence> uris = new ArrayList<>();
            for (int i = 0; i < selectedFileInfos.size(); i++) {
                File file = new File(selectedFileInfos.get(i).getUri());
                if (file.isDirectory()) {
                    File[] files = file.listFiles(File::isFile);
                    if (files == null) {
                        continue;
                    }
                    for (File child : files) {
                        uris.add(child.getPath());
                    }
                } else {
                    uris.add(file.getPath());
                }
            }
            setResult(RESULT_OK, new Intent().putCharSequenceArrayListExtra("files", uris));
            finish();
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void getFiles() {
        getFiles(pathList.get(pathList.size() - 1));
    }

    private void getFiles(String path) {
        fileInfos.clear();
        File file = new File(path);
        ActionBar toolBar = getSupportActionBar();
        assert toolBar != null;
        if (ROOT.equals(path)) {
            toolBar.setTitle("内部储存");
        } else {
            toolBar.setTitle(file.getName());
        }
        toolBar.setSubtitle(path);
        binding.refreshLayout.setRefreshing(true);
        new Thread(() -> {
            File[] files = file.listFiles();
            if (files == null) {
                runOnUiThread(() -> {
                    fileSelectorAdapter.notifyDataSetChanged();
                    binding.refreshLayout.setRefreshing(false);
                });
                return;
            }
            for (File child : files) {
                String name = child.getName();
                FileInfo fileInfo = new FileInfo(name, child.getPath(), 0L);
                if (child.isFile()) {
                    long length = child.length();
                    fileInfo.setLength(length);
                    String extension = ando.file.core.FileUtils.INSTANCE.getExtension(name);
                    fileInfo.setExtension(extension);
                    fileInfo.setFileDesc(FileUtils.toSize(length));
                } else {
                    fileInfo.setFileDesc("文件夹");
                }
                if (selectedFileInfos.contains(fileInfo)) {
                    fileInfo.setSelected(true);
                }
                fileInfos.add(fileInfo);
            }
            Collections.sort(fileInfos, new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo o1, FileInfo o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            runOnUiThread(() -> {
                fileSelectorAdapter.notifyDataSetChanged();
                binding.refreshLayout.setRefreshing(false);
            });
        }).start();
    }
}
