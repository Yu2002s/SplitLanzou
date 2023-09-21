package com.lanzou.cloud;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.google.android.material.color.DynamicColors;

import org.litepal.LitePal;

import ando.file.core.FileOperator;

public class LanzouApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context context;

    public static final String HOST = "https://pc.woozooo.com/";
    public static final String HOST_FILE = HOST + "mydisk.php";

    public static final String HOST_LOGIN = HOST + "account.php?action=login";

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        DynamicColors.applyToActivitiesIfAvailable(this);
        LitePal.initialize(this);
        FileOperator.INSTANCE.init(this, false);
    }
}
