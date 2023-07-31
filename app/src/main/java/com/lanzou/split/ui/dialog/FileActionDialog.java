package com.lanzou.split.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;

import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.databinding.DialogFileActionBinding;
import com.lanzou.split.utils.DisplayUtils;

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
