package com.lanzou.cloud.utils;

import android.content.Context;
import android.util.DisplayMetrics;

import com.lanzou.cloud.LanzouApplication;

public class DensityUtils {

    /**
     * dp 转 px
     *
     * @param dp dp值
     * @return 像素值
     */
    public static int dp2px(int dp) {
        Context context = LanzouApplication.context;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (displayMetrics.density * dp + 0.5);
    }

}
