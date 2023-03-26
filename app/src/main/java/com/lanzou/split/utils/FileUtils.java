package com.lanzou.split.utils;

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

}
