package com.lanzou.cloud.ui.selector;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.adapter.FileSelectorAdapter;
import com.lanzou.cloud.data.FileInfo;
import com.lanzou.cloud.databinding.ActivityPhoneFileBinding;
import com.lanzou.cloud.event.OnItemClickListener;
import com.lanzou.cloud.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 手机文件选择器
 */
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

        fileSelectorAdapter.setOnItemClickListener((position, view) -> {
            FileInfo fileInfo = fileInfos.get(position);
            if (fileInfo)
            getFiles(fileInfo.getUri());
        });
    }

    private void getFiles(String path) {
        fileInfos.clear();
        new Thread(() -> {
            File file = new File(path);
            File[] files = file.listFiles();
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
