package com.lanzou.split.ui.web;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.lanzou.split.LanzouApplication;
import com.lanzou.split.data.User;
import com.lanzou.split.databinding.ActivityWebviewBinding;
import com.lanzou.split.network.Repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WebActivity extends AppCompatActivity {

    private ActivityWebviewBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.header.toolBar;
        // toolbar.setNavigationOnClickListener(v -> finish());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initWebView();
    }

    @Override
    public void onBackPressed() {
        WebView webView = binding.webView;
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void initWebView() {
        WebView webView = binding.webView;

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setSaveFormData(true);
        settings.setUserAgentString("PC");

        String url = getIntent().getStringExtra("url");
        webView.loadUrl(url);
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.addJavascriptInterface(new HtmlSourceObj(), "local_obj");
    }

    private class HtmlSourceObj {
        @JavascriptInterface
        public void saveUser(long uid, String username, String cookie) {
            runOnUiThread(() -> new MaterialAlertDialogBuilder(WebActivity.this)
                    .setCancelable(false)
                    .setTitle("保存用户信息")
                    .setMessage("将对登录信息进行保存在本地，不会对个人信息进行上传云端，请放心使用")
                    .setNeutralButton("关闭", null)
                    .setPositiveButton("确认保存", (dialog, which) -> {
                        User user = new User();
                        user.setUid(uid);
                        user.setCookie(cookie);
                        user.setUsername(username);
                        user.setCurrent(true);
                        Repository.getInstance().saveOrUpdateUser(user);
                        CookieManager.getInstance().removeAllCookies(null);
                        setResult(RESULT_OK);
                        finish();
                    }).setNegativeButton("退出", (dialog, which) -> finish()).show());

        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http") || url.startsWith("https")) {
                return false;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (url.equals(LanzouApplication.HOST_FILE)) {
                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager.getCookie(url);
                if (cookie.contains("phpdisk_info=")) {
                    String jsStr = getJsStr();
                    if (jsStr == null) {
                        return;
                    }
                    view.loadUrl("javascript:" + jsStr);
                }
            }
        }
    }

    @Nullable
    private String getJsStr() {
        try {
            InputStream inputStream = getAssets().open("js/user.js");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            byte[] bytes = new byte[1024];
            while ((len = inputStream.read(bytes)) != -1) {
                bos.write(bytes, 0, len);
            }
            String str = bos.toString();
            bos.close();
            inputStream.close();
            return str;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            LinearProgressIndicator progressIndicator = binding.progressBar;
            if (newProgress == 100) {
                progressIndicator.setVisibility(View.INVISIBLE);
            } else {
                progressIndicator.setVisibility(View.VISIBLE);
                progressIndicator.setProgress(newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            getSupportActionBar().setTitle(title);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            // 处理JS请求
            return super.onJsAlert(view, url, message, result);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.webView.destroy();
    }
}
