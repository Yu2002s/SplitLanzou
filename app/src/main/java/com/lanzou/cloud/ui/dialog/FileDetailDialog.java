package com.lanzou.cloud.ui.dialog;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.data.LanzouSimpleResponse;
import com.lanzou.cloud.data.LanzouUrl;
import com.lanzou.cloud.databinding.DialogFileDetailBinding;
import com.lanzou.cloud.network.Repository;

public class FileDetailDialog extends MaterialAlertDialogBuilder {

    private final long fileId;

    private boolean isFile = false;

    private DialogFileDetailBinding binding;

    private ClipboardManager clipboardManager;

    public FileDetailDialog setFileName(String name) {
        if (this.fileId <= 0) {
            return this;
        }
        binding.tvName.setText(name);
        return this;
    }

    public FileDetailDialog(@NonNull Context context, long fileId) {
        this(context, fileId, false);
    }

    public FileDetailDialog(@NonNull Context context, long fileId, boolean isFile) {
        super(context);
        this.fileId = fileId;
        this.isFile = isFile;
        if (fileId <= 0) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DialogFileDetailBinding.inflate(inflater);
        setView(binding.getRoot());
        setTitle("文件详情");
        binding.btnShareDownload.setVisibility(isFile ? View.VISIBLE : View.GONE);

        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Activity activity = (Activity) context;

        new Thread(() -> {
            LanzouUrl lanzouUrl;
            if (isFile) {
                lanzouUrl = Repository.getInstance().getLanzouUrl(fileId);
            } else {
                lanzouUrl = Repository.getInstance().getFolder(fileId);
            }
            if (lanzouUrl == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                if (isFile) {
                    binding.editUrl.setText(lanzouUrl.getHost() + "/" + lanzouUrl.getFileId());
                } else {
                    binding.editUrl.setText(lanzouUrl.getUrl());
                }
                binding.editPwd.setText(lanzouUrl.getPwd());
                binding.swPwd.setChecked(lanzouUrl.getHasPwd() == 1);
            });
        }).start();

        AlertDialog alertDialog = create();
        alertDialog.show();

        binding.btnClose.setOnClickListener(v -> alertDialog.dismiss());

        binding.btnShare.setOnClickListener(v -> {
            DialogInterface.OnClickListener clickListener = (dialog, which) -> {
                String url = binding.editUrl.getText().toString();
                if (url.isEmpty()) {
                    return;
                }
                String id = url.substring(url.lastIndexOf("/") + 1);
                String shareUrl;
                if (isFile) {
                    if (which == 0) {
                        shareUrl = LanzouApplication.SHARE_URL2 + id;
                        if (binding.swPwd.isChecked()) {
                            shareUrl += "/" + binding.editPwd.getText().toString();
                        }
                    } else {
                        shareUrl = url;
                    }
                } else {
                    if (which == 0) {
                        shareUrl = LanzouApplication.SHARE_FOLDER_URL + id;
                        if (binding.swPwd.isChecked()) {
                            shareUrl += "/" + binding.editPwd.getText().toString();
                        }
                    } else {
                        shareUrl = url;
                    }
                }
                clipboardManager.setPrimaryClip(ClipData.newPlainText("url", shareUrl));
                Toast.makeText(context, "分享地址已复制到剪切板", Toast.LENGTH_SHORT).show();
            };

            new MaterialAlertDialogBuilder(context)
                    .setItems(new CharSequence[]{"自定义分享地址", "原始分享地址"}, clickListener)
                    .setPositiveButton("关闭", null)
                    .show();
        });

        View.OnClickListener onClickListener = v -> new Thread(() -> {
            String pwd = binding.editPwd.getText().toString();

            LanzouSimpleResponse lanzouSimpleResponse;

            if (isFile) {
                lanzouSimpleResponse = Repository.getInstance()
                        .editFilePassword(fileId, binding.swPwd.isChecked(), pwd);
            } else {
                lanzouSimpleResponse = Repository.getInstance()
                        .editFolderPassword(fileId, binding.swPwd.isChecked(), pwd);
            }

            if (lanzouSimpleResponse == null) {
                return;
            }

            if (lanzouSimpleResponse.getStatus() != 1) {
                activity.runOnUiThread(() -> binding.swPwd.setChecked(!binding.swPwd.isChecked()));
            }
            Looper.prepare();
            Toast.makeText(context, lanzouSimpleResponse.getInfo(), Toast.LENGTH_SHORT).show();
            Looper.loop();
        }).start();

        binding.swPwd.setOnClickListener(onClickListener);
        binding.btnSave.setOnClickListener(onClickListener);

        binding.btnShareDownload.setOnClickListener(v -> {
            String url = binding.editUrl.getText().toString();
            if (binding.swPwd.isChecked()) {
                url += "&pwd=" + binding.editPwd.getText().toString();
            }
            String downloadUrl = "http://api.jdynb.xyz:6400/parser?url=" + url;
            clipboardManager.setPrimaryClip(ClipData.newPlainText("url", downloadUrl));
            Toast.makeText(context, "下载直链已复制到剪切板", Toast.LENGTH_SHORT).show();
        });
    }
}
