package com.lanzou.cloud.ui.dialog;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.data.LanzouSimpleResponse;
import com.lanzou.cloud.data.LanzouUrlResponse;
import com.lanzou.cloud.databinding.DialogFileDetailBinding;
import com.lanzou.cloud.network.Repository;

public class FileDetailDialog extends MaterialAlertDialogBuilder {

    private final long fileId;

    private DialogFileDetailBinding binding;

    private ClipboardManager clipboardManager;

    public FileDetailDialog(@NonNull Context context, long fileId) {
        super(context);
        this.fileId = fileId;
        if (fileId <= 0) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DialogFileDetailBinding.inflate(inflater);
        setView(binding.getRoot());
        setTitle("文件详情");

        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Activity activity = (Activity) context;

        new Thread(() -> {
            LanzouUrlResponse lanzouUrlResponse = Repository.getInstance().getFolder(fileId);
            if (lanzouUrlResponse.getInfo() == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                binding.editUrl.setText(lanzouUrlResponse.getInfo().getUrl());
                binding.editPwd.setText(lanzouUrlResponse.getInfo().getPwd());
                binding.swPwd.setChecked(lanzouUrlResponse.getInfo().getHasPwd() == 1);
            });
        }).start();

        AlertDialog alertDialog = create();
        alertDialog.show();

        binding.btnClose.setOnClickListener(v -> alertDialog.dismiss());

        binding.btnShare.setOnClickListener(v -> {
            String shareUrl = LanzouApplication.SHARE_URL + "?url=" + binding.editUrl.getText().toString();
            if (binding.swPwd.isChecked()) {
                shareUrl += "&pwd=" + binding.editPwd.getText().toString();
            }
            clipboardManager.setPrimaryClip(ClipData.newPlainText("url", shareUrl));
            Toast.makeText(context, "分享地址已复制到剪切板", Toast.LENGTH_SHORT).show();
        });

        binding.swPwd.setOnClickListener(v -> {
            new Thread(() -> {
                String pwd = binding.editPwd.getText().toString();
                LanzouSimpleResponse lanzouSimpleResponse = Repository.getInstance().editFilePassword(fileId, binding.swPwd.isChecked(), pwd);
                if (lanzouSimpleResponse.getStatus() != 1) {
                    activity.runOnUiThread(() -> binding.swPwd.setChecked(!binding.swPwd.isChecked()));
                }
                Looper.prepare();
                Toast.makeText(context, lanzouSimpleResponse.getInfo(), Toast.LENGTH_SHORT).show();
                Looper.loop();
            }).start();
        });
    }
}
