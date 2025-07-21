package com.lanzou.cloud.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lanzou.cloud.LanzouApplication;

public class SpUtils {

    private static final SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(LanzouApplication.context);

    private static final SharedPreferences.Editor editor = sharedPreferences.edit();

    public static void save(@NonNull String key, String value) {
        editor.putString(key, value);
    }

    @Nullable
    public static String get(@NonNull String key, @Nullable String def) {
        return sharedPreferences.getString(key, def);
    }

    public static Long getLong(@NonNull String key) {
        return getLong(key, 0L);
    }

    public static Long getLong(@NonNull String key, Long def) {
        return sharedPreferences.getLong(key, def);
    }



}
