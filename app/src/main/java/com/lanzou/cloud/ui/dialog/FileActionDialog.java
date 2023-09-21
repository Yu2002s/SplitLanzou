package com.lanzou.cloud.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.lanzou.cloud.databinding.DialogFileActionBinding;

public class FileActionDialog extends AlertDialog {

    private DialogFileActionBinding binding;

    public FileActionDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogFileActionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvName.setText("啊哈哈哈哈哈");
        binding.tvDesc.setText("啊哈哈哈哈");

        setButton(BUTTON_POSITIVE, "123", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
    }
}
