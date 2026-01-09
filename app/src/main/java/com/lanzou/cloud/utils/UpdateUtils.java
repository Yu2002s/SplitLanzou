package com.lanzou.cloud.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.pm.PackageInfoCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.LanzouApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * App 更新工具类
 */
public class UpdateUtils {

    private static final String TAG = "UpdateUtils";

    /**
     * 更新地址（github pages 部署的静态 json 地址）
     */
    @Deprecated
    private static final String URL = "https://yu2002s.github.io/SplitLanzou/version.json";
    private static final String URL_GITEE = "https://gitee.com/jdy2002/SplitLanzou/raw/master/version.json";

    /**
     * 检查更新
     *
     * @param context activity
     */
    public static void checkUpdate(Activity context) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL_GITEE)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                context.runOnUiThread(() ->
                        Toast.makeText(context, "网络被墙，检查更新失败，可前往设置手动获取更新\n" + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.i(TAG, "update -> responseBody is null.");
                    return;
                }
                String json = body.string();
                handleUpdate(context, json);
            }
        });
    }

    /**
     * 处理更新
     *
     * @param context activity
     * @param json    返回的 json 内容
     */
    private static void handleUpdate(Activity context, String json) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long currentVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo);

            JSONObject jsonObject = new JSONObject(json);
            int versionCode = jsonObject.getInt("versionCode");
            String versionName = jsonObject.getString("version");
            String content = jsonObject.getString("content");
            String url = jsonObject.getString("url");
            context.runOnUiThread(() -> {
                Log.i(TAG, "更新信息: " + json);
                if (versionCode > currentVersionCode) {
                    Log.i(TAG, "发现新版本: " + versionName);
                    showUpdateDialog(context, versionName, content, url);
                }
            });
        } catch (JSONException | PackageManager.NameNotFoundException e) {
            Log.e(TAG, "处理更新失败: " + e.getMessage());
        }
    }

    /**
     * 显示更新弹窗
     *
     * @param context     activity
     * @param versionName 版本名
     * @param content     更新内容
     * @param url         更新地址
     */
    private static void showUpdateDialog(Activity context, String versionName, String content, String url) {
        // 需要更新了
        new MaterialAlertDialogBuilder(context)
                .setTitle("发现新版本-" + versionName)
                .setMessage(content)
                .setPositiveButton("取消", null)
                .setNeutralButton("Github主页", ((dialog, which) -> {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LanzouApplication.GITHUB_HOME)));
                }))
                .setNegativeButton("更新", (dialog, which) ->
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                ).show();
    }
}
