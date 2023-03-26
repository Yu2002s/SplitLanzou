package com.lanzou.split.ui.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lanzou.split.base.BaseActivity;
import com.lanzou.split.data.Download;
import com.lanzou.split.data.Upload;
import com.lanzou.split.databinding.ActivityDownloadInfoBinding;
import com.lanzou.split.event.OnDownloadListener;
import com.lanzou.split.service.DownloadService;
import com.lanzou.split.utils.FileUtils;

public class DownloadInfoActivity extends BaseActivity implements OnDownloadListener, ServiceConnection {

    private ActivityDownloadInfoBinding binding;
    private DownloadService downloadService;

    public static void actionStart(Context context, Download download) {
        Intent intent = new Intent(context, DownloadInfoActivity.class);
        intent.putExtra("download", download);
        context.startActivity(intent);
    }

    private Download download;

    private int currentStatus = -2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        download = getIntent().getParcelableExtra("download");
        bindService(new Intent(this, DownloadService.class), this, BIND_AUTO_CREATE);
        binding = ActivityDownloadInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(download.getName());
        Upload upload = download.getUpload();
        addLog("文件名: " + download.getName()
        + "\n文件大小: " + download.getLength()
        + "\n下载状态: " + download.getStatusStr()
        + "\n区块大小: " + (upload == null ? 0: upload.getFiles().size())
        + "\n当前进度: " + download.getCurrent() + "/" + download.getLength() + "\n");
        updateStatus();

        binding.btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadService.toggleDownload(download);
            }
        });
    }

    private void updateStatus() {
        if (currentStatus != download.getStatus()) {
            currentStatus = download.getStatus();
            // 这里更新状态信息
            addLog("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
            if (download.isComplete()) {
                addLog("\n文件已保存到"+ download.getPath() + "\n");
                binding.btnToggle.setText("打开文件");
            } else {
                if (download.isDownload()) {
                    binding.btnToggle.setText("暂停下载");
                } else {
                    binding.btnToggle.setText("继续下载");
                }
            }
        }
        getSupportActionBar().setSubtitle(download.getStatusStr() + " "
                + FileUtils.toSize(download.getSpeed()) + "/s " + download.getProgress()
                + "% " + FileUtils.toSize(download.getLength()));

    }

    private void addLog(String content) {
        binding.tvLog.append(content);
        binding.scrollView.fullScroll(View.FOCUS_DOWN);
    }

    @Override
    public void onDownload(Download download) {
        if (!this.download.equals(download)) {
            return;
        }
        if (this.download != download) {
            this.download = download;
        }
        addLog(download.getStatusStr() + " => " + download.getCurrent()
                + "/" + download.getLength() + " (" + download.getProgress() + "%)\n");
        updateStatus();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.DownloadBinder)service).getService();
        downloadService.addDownloadListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        downloadService.removeDownloadListener(this);
    }
}
