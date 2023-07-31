package com.lanzou.split.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.LanzouApplication;
import com.lanzou.split.adapter.SimpleListAdapter;
import com.lanzou.split.data.User;
import com.lanzou.split.network.Repository;
import com.lanzou.split.ui.web.WebActivity;

import java.util.ArrayList;
import java.util.List;

public class UserDialog extends AlertDialog.Builder {
    public UserDialog(@NonNull Context context) {
        super(context);

        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setView(recyclerView);

        setTitle("账号管理");
        Repository repository = Repository.getInstance();
        List<User> userList = repository.getSavedUserList();

        String[] list = new String[userList.size()];
        int position = 0;
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            list[i] = user.getUsername();
            if (user.isCurrent()) {
                position = i;
            }
        }

        setSingleChoiceItems(list, position, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                repository.selectUser(userList.get(which));
                dialog.dismiss();
                Toast.makeText(context, "切换账号后请返回主页根目录刷新", Toast.LENGTH_SHORT).show();
            }
        });

        setPositiveButton("添加账号", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                context.startActivity(new Intent(context, WebActivity.class)
                        .putExtra("url", LanzouApplication.HOST_LOGIN));
            }
        });
    }
}
