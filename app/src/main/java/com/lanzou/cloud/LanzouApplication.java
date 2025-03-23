package com.lanzou.cloud;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import org.litepal.LitePal;

import ando.file.core.FileOperator;

/**
 * 第三方蓝奏云 (lanzou.com)
 * 支持上传 100M+ 文件
 */
public class LanzouApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context context;

    public static final String HOST = "https://pc.woozooo.com/";
    public static final String HOST_FILE = HOST + "mydisk.php";

    public static final String HOST_LOGIN = HOST + "account.php?action=login";

    public static final String API_URL = "http://api.jdynb.xyz:6400";

    public static final String SHARE_URL = "http://lz.jdynb.xyz/index.html";

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        LitePal.initialize(this);
        FileOperator.INSTANCE.init(this, false);
    }
}
