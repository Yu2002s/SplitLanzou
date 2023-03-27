package com.lanzou.split.ui.resolve;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AndroidException;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lanzou.split.base.BaseActivity;
import com.lanzou.split.databinding.ActivityResolveFileBinding;
import com.lanzou.split.service.DownloadService;

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
