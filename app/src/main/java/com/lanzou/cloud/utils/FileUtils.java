package com.lanzou.cloud.utils;

import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.lanzou.cloud.LanzouApplication;

import java.io.File;
import java.text.DecimalFormat;

public class FileUtils {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    public static String toSize(long length) {
        if (length < 1024) {
            return length + "B";
        } else if (length < 1048576) {
            return df.format(length / 1024f) + "KB";
        } else if (length < 1073741824) {
            return df.format(length / 1048576f) + "MB";
        } else {
            return df.format(length / 1073741824f) + "GB";
        }
    }

    public static boolean openFile(File file) {
        String name = file.getName();
        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(LanzouApplication.context, "com.lanzou.cloud.fileProvider", file);
            String ext = name.substring(name.lastIndexOf(".") + 1);
            String mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(ext);
            if (mimeType == null) {
                mimeType = "*/*";
            }
            intent.setDataAndType(uri, mimeType);
            LanzouApplication.context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static boolean openFile(String path) {
        if (path == null) {
            return false;
        }
        File file = new File(path);
        return openFile(file);
    }
}
