package com.lanzou.cloud.ui.setting;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.MainActivity;
import com.lanzou.cloud.base.BaseActivity;
import com.lanzou.cloud.databinding.ActivitySettingBinding;
import com.lanzou.cloud.databinding.ItemListSettingBinding;
import com.lanzou.cloud.event.OnItemClickListener;
import com.lanzou.cloud.network.Repository;
import com.lanzou.cloud.ui.LinearItemDecoration;
import com.lanzou.cloud.ui.folder.FolderSelectorActivity;
import com.lanzou.cloud.ui.question.QuestionActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends BaseActivity {

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySettingBinding binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        List<String> list = new ArrayList<>();
        list.add("获取权限");
        list.add("修改路径");
        list.add("一些问题");
        list.add("获取更新");
        list.add("关于APP");

        list.add("退出登录");
        RecyclerView rv = binding.settingRv;
        rv.addItemDecoration(new LinearItemDecoration());
        rv.setLayoutManager(new LinearLayoutManager(this));
        ArrayAdapter arrayAdapter = new ArrayAdapter(list);
        rv.setAdapter(arrayAdapter);

        launcher = registerForActivityResult(new ActivityResultContract<Intent, Long>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Intent intent) {
                return intent;
            }

            @Override
            public Long parseResult(int i, @Nullable Intent intent) {
                if (intent == null) {
                    return 0L;
                }
                return intent.getLongExtra("id", 0);
            }
        }, result -> {
            if (result == 0) {
                return;
            }
            Repository.getInstance().updateUploadPath(result);
            Toast.makeText(SettingActivity.this, "已选择缓存路径", Toast.LENGTH_SHORT).show();
        });

        arrayAdapter.setItemClickListener((position, view) -> {
            switch (position) {
                case 0:
                    requestPermission();
                    break;
                case 1:
                    showUploadDialog();
                    break;
                case 2:
                    // 一些问题
                    startActivity(new Intent(SettingActivity.this, QuestionActivity.class));
                    break;
                case 3:
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(LanzouApplication.GITHUB_HOME));
                    startActivity(intent);
                    break;
                case 4:
                    // 关于
                    showAboutDialog();
                    break;
                case 5:
                    Repository.getInstance().logout();
                    System.exit(0);
                    startActivity(new Intent(SettingActivity.this, MainActivity.class));
                    break;
            }
        });
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("关于")
                .setMessage("软件仅供学习交流，请勿用于其他用途。不会自动更新，如需获取其他信息请访问github主页，如有问题请提issue\n\n作者:Yu2002s")
                .setPositiveButton("关闭", null)
                .setNeutralButton("github主页", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(LanzouApplication.GITHUB_HOME));
                    startActivity(intent);
                })
                .show();
    }

    private void showUploadDialog() {
        // 未选择上传目录
        new MaterialAlertDialogBuilder(this)
                .setTitle("选择缓存位置")
                .setMessage("选择缓存上传文件的位置，这是必须设置项，注意，此目录必须为不使用目录，因为将会上传大量缓存文件到此处")
                .setNegativeButton("先不选", null)
                .setPositiveButton("去选择", (dialog, which)
                        -> launcher.launch(new Intent(SettingActivity.this, FolderSelectorActivity.class))).show();

    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Toast.makeText(this, "请授权此权限", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignore) {
                }
            }
        } else {
            int granted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, 1);
            }
        }
    }

    private static class ArrayAdapter extends RecyclerView.Adapter<ArrayAdapter.ViewHolder> {

        private final List<String> list;

        private OnItemClickListener itemClickListener;

        public void setItemClickListener(OnItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        public ArrayAdapter(List<String> list) {
            this.list = list;
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {

            final ItemListSettingBinding binding;

            public ViewHolder(@NonNull ItemListSettingBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        @NonNull
        @Override
        public ArrayAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemListSettingBinding binding = ItemListSettingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            ViewHolder viewHolder = new ViewHolder(binding);
            binding.getRoot().setOnClickListener(v -> {
                int position = viewHolder.getAbsoluteAdapterPosition();
                itemClickListener.onItemClick(position, v);
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ArrayAdapter.ViewHolder holder, int position) {
            holder.binding.tvTitle.setText(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }


    }

}
