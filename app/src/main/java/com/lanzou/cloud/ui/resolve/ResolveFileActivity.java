package com.lanzou.cloud.ui.resolve;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lanzou.cloud.databinding.ActivityResolveFileBinding;
import com.lanzou.cloud.service.DownloadService;
import com.lanzou.cloud.ui.activity.ResolveFolderActivity;

public class ResolveFileActivity extends AppCompatActivity implements ServiceConnection {

    private DownloadService downloadService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, DownloadService.class), this, BIND_AUTO_CREATE);
        ActivityResolveFileBinding binding = ActivityResolveFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.btnResolve.setOnClickListener(v -> {
            String url = binding.editUrl.getText().toString();
            String pwd = binding.editPwd.getText().toString();
            resolveFile(url, pwd);
        });

        CharSequence charSequenceExtra = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        // boolean readonly = getIntent().getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);
        if (charSequenceExtra != null) {
            binding.editUrl.setText(charSequenceExtra);
        }

        Uri data = getIntent().getData();
        if (data != null) {
            String pwd = data.getQueryParameter("pwd");
            if (!TextUtils.isEmpty(pwd) && !"null".equals(pwd)) {
                binding.editPwd.setText(pwd);
            }
            String url = "https://" + data.getHost() + data.getPath();
            binding.editUrl.setText(url);
        }

        binding.btnResolveFolder.setOnClickListener(view -> {
            startActivity(new Intent(this, ResolveFolderActivity.class));
        });
    }

    private void resolveFile(String url, @Nullable String pwd) {
        downloadService.addDownload(url, null, pwd);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.DownloadBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        downloadService = null;
    }
}
