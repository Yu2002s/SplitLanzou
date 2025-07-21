package com.lanzou.cloud.ui.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.lanzou.cloud.base.java.BaseActivity;
import com.lanzou.cloud.data.Download;
import com.lanzou.cloud.data.Upload;
import com.lanzou.cloud.databinding.ActivityDownloadInfoBinding;
import com.lanzou.cloud.event.OnDownloadListener;
import com.lanzou.cloud.service.DownloadService;
import com.lanzou.cloud.utils.FileJavaUtils;

public class DownloadInfoActivity extends BaseActivity implements OnDownloadListener, ServiceConnection {

    private ActivityDownloadInfoBinding binding;
    private DownloadService downloadService;

    public static void actionStart(Context context, ActivityResultLauncher<Intent> launcher, Download download, int position) {
        Intent intent = new Intent(context, DownloadInfoActivity.class);
        intent.putExtra("download", download);
        intent.putExtra("position", position);
        launcher.launch(intent);
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

        binding.btnToggle.setOnClickListener(v -> downloadService.toggleDownload(download));

        ViewCompat.setOnApplyWindowInsetsListener(binding.btnToggle, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.bottomMargin = bottom;
                return insets;
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
                + FileJavaUtils.toSize(download.getSpeed()) + "/s " + download.getProgress()
                + "% " + FileJavaUtils.toSize(download.getLength()));

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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "删除文件");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 0) {
            downloadService.removeDownload(download);
            setResult(RESULT_OK, new Intent().putExtra("position", getIntent().getIntExtra("position", -1)));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        downloadService.removeDownloadListener(this);
    }
}
