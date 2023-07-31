package com.lanzou.split.ui.selector;

import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.adapter.FileSelectorAdapter;
import com.lanzou.split.data.FileInfo;
import com.lanzou.split.databinding.ActivityPhoneFileBinding;
import com.lanzou.split.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ando.file.core.FileUri;

public class PhoneFileActivity extends AppCompatActivity {

    private ActivityPhoneFileBinding binding;

    private final List<FileInfo> fileInfos = new ArrayList<>();

    private final FileSelectorAdapter fileSelectorAdapter = new FileSelectorAdapter(fileInfos);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPhoneFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RecyclerView recyclerView = binding.fileRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fileSelectorAdapter);

        getFiles(Environment.getExternalStorageDirectory().getPath());
    }

    private void getFiles(String path) {
        fileInfos.clear();
        new Thread(() -> {
            File file = new File(path);
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
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
                }
                fileInfos.add(fileInfo);
            }
            runOnUiThread(() -> fileSelectorAdapter.notifyDataSetChanged());
        }).start();
    }
}
