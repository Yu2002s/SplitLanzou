package com.lanzou.cloud.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.pm.PackageInfoCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UpdateUtils {

    private static final String URL = "https://yu2002s.github.io/SplitLanzou/version.json";

    public static void checkUpdate(Activity context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long currentVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo);
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(URL)
                    .build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "网络被墙，检查更新失败\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    ResponseBody body = response.body();
                    if (body == null) {
                        return;
                    }
                    String json = body.string();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        int versionCode = jsonObject.getInt("versionCode");
                        String versionName = jsonObject.getString("version");
                        String content = jsonObject.getString("content");
                        String url = jsonObject.getString("url");
                        context.runOnUiThread(() -> {
                            if (versionCode > currentVersionCode) {
                                // 需要更新了
                                new AlertDialog.Builder(context)
                                        .setTitle("发现新版本-" + versionName)
                                        .setMessage(content)
                                        .setPositiveButton("取消", null)
                                        .setNegativeButton("更新", (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))).show();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
