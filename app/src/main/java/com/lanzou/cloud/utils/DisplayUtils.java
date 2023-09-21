package com.lanzou.cloud.utils;

import android.content.Context;
import android.os.Build;
import android.view.WindowManager;

import com.lanzou.cloud.LanzouApplication;

public class DisplayUtils {

    public static int getWindowWidth() {
        WindowManager windowManager = (WindowManager) LanzouApplication.context
                .getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return windowManager.getCurrentWindowMetrics().getBounds().width();
        }
        return windowManager.getDefaultDisplay().getWidth();
    }

}
