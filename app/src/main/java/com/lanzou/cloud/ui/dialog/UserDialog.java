package com.lanzou.cloud.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.data.User;
import com.lanzou.cloud.network.Repository;
import com.lanzou.cloud.ui.web.WebActivity;

import java.util.List;

public class UserDialog extends MaterialAlertDialogBuilder {
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

        setSingleChoiceItems(list, position, (dialog, which) -> {
            repository.selectUser(userList.get(which));
            dialog.dismiss();
            Toast.makeText(context, "切换账号后请返回主页根目录刷新", Toast.LENGTH_SHORT).show();
        });

        setPositiveButton("添加账号", (dialog, which) -> context.startActivity(new Intent(context, WebActivity.class)
                .putExtra("url", LanzouApplication.HOST_LOGIN)));
    }
}
