package com.lanzou.split.ui.upload;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.lanzou.split.R;
import com.lanzou.split.base.BaseActivity;
import com.lanzou.split.data.SplitFile;
import com.lanzou.split.data.Upload;
import com.lanzou.split.databinding.ActivityUploadInfoBinding;
import com.lanzou.split.event.OnUploadListener;
import com.lanzou.split.service.UploadService;
import com.lanzou.split.utils.FileUtils;

public class UploadInfoActivity extends BaseActivity implements ServiceConnection, OnUploadListener {

    private ActivityUploadInfoBinding binding;

    private Toolbar toolbar;

    private UploadService uploadService;

    private Upload upload;

    private int currentStatus;

    private int currentIndex = -1;

    public static void start(Context context, Upload upload) {
        Intent intent = new Intent(context, UploadInfoActivity.class);
        intent.putExtra("upload", upload);
        context.startActivity(intent);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        uploadService = ((UploadService.UploadBinder)service).getService();
        uploadService.addUploadListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        uploadService.removeUploadListener(this);
    }

    @Override
    public void onUpload(Upload upload) {
        if (!this.upload.equals(upload)) {
            return;
        }
        if (this.upload != upload) {
            this.upload = upload;
            currentStatus = upload.getStatus();
        }
        updateStatus();
        addLog(upload.getStatusStr() + " => current: "
                + upload.getCurrent() + "/" + upload.getLength()
                + " (" + upload.getProgress() + "%)\n");
        updateSpitFileIndex();
    }

    private void updateSpitFileIndex() {
        if (upload.getFiles() == null || upload.getFiles().isEmpty()) {
            return;
        }
        if (currentIndex != upload.getIndex()) {
            currentIndex = upload.getIndex();
            SplitFile splitFile = upload.getFiles().get(currentIndex);
            if (currentIndex > 0) {
                int prevIndex = currentIndex - 1;
                addSplitFileIndexLog(prevIndex);
            }
            addLog("->->-> 开始第[" + currentIndex + "]区块上传 <-<-<-" +
                    "\n=> start: " + splitFile.getStart() + " -> " + splitFile.getEnd() +
                    "\n=> 区块大小: " + splitFile.getLength() + "(" + FileUtils.toSize(splitFile.getLength()) + ")" +
                    "\n===============================\n");
        }
        if (upload.isComplete()) {
            addSplitFileIndexLog(upload.getIndex());
        }
    }

    private void addSplitFileIndexLog(int index) {
        SplitFile prevSplitFile = upload.getFiles().get(index);
        addLog("->->-> 已结束[" + index + "]区块上传 <-<-<-" +
                "\n=> 下载地址: " + prevSplitFile.getUrl()
                + "\n=> 累计上传: " + upload.getCurrent() + "("+ FileUtils.toSize(upload.getCurrent()) + ")\n");
    }

    private void updateStatus() {
        toolbar.setSubtitle(upload.getStatusStr() + " "
                +  FileUtils.toSize(upload.getSpeed())
                + "/s " + upload.getProgress() + "% " + upload.getSize());
        if (currentStatus != upload.getStatus()) {
            // 状态变更了
            currentStatus = upload.getStatus();
            addLog("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
            if (upload.isComplete()) {
                binding.btnToggle.setText("文件已上传");
            } else {
                if (upload.isUpload()) {
                    binding.btnToggle.setText("停止上传");
                } else {
                    binding.btnToggle.setText("继续上传");
                }
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, UploadService.class), this, BIND_AUTO_CREATE);
        upload = getIntent().getParcelableExtra("upload");
        binding = ActivityUploadInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        toolbar = binding.header.toolBar;
        toolbar.setTitle(upload.getName());
        updateStatus();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d("jdy", upload.toString());
        addLog("文件名: " + upload.getName()
                + "\n文件大小: " + upload.getSize()
                + "\n分割区块: " + upload.getFiles().size() + "(未确定)"
                + "\n文件路径: " + upload.getPath()
                + "\n上传到: " + upload.getUploadPage().getName()
                + "\n上传进度: " + upload.getProgress() + "%"
                + "\n状态: " + upload.getStatusStr() + "\n\n"
                + "===================上传日志=================\n\n");

        binding.btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadService.toggleUpload(upload);
            }
        });
    }

    private void addLog(String log) {
        binding.tvLog.append(log);
        binding.scrollView.fullScroll(View.FOCUS_DOWN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadService.removeUploadListener(this);
        unbindService(this);
        uploadService = null;
    }
}
