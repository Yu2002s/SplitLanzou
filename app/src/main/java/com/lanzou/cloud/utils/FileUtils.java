package com.lanzou.cloud.utils;

import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.data.FileInfo;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class FileUtils {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private static final String ROOT_PATH = "/storage/emulated/0";

    /**
     * 下载文件目录
     */
    private static final String DOWNLOAD_PATH = ROOT_PATH + "/Download";

    private static final String DCIM_PATH = ROOT_PATH + "/DCIM";

    private static final String ANDROID_PATH = ROOT_PATH + "/Android";

    private static final String PICTURES_PATH = ROOT_PATH + "/Pictures";

    private static final String DOCUMENTS_PATH = ROOT_PATH + "/Documents";

    private static final String MIUI_PATH = ROOT_PATH + "/MIUI";

    public static final String QQ_PATH = ROOT_PATH + "/QQ";

    public static final String WECHAT_PATH = ROOT_PATH + "/WeiXin";

    /**
     * 获取文件大小 + 单位
     *
     * @param length 文件大小
     * @return + 单位
     */
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

    /**
     * 获取文件夹的中文名
     *
     * @param path 文件路径
     * @return 中文名称
     */
    public static String getFolderChineseName(String path) {
        switch (path) {
            case DOWNLOAD_PATH:
                return "下载的文件";
            case DCIM_PATH:
                return "相册、视频、截图";
            case DOCUMENTS_PATH:
                return "文档目录";
            case PICTURES_PATH:
                return "App 保存的图片";
            case QQ_PATH:
                return "QQ保存的文件";
            case WECHAT_PATH:
                return "微信保存的文件";
            case MIUI_PATH:
                return "MIUI 备份、主题、录音等文件";
            case ANDROID_PATH:
                return "Android 系统文件夹, 包含 App数据等";
        }
        return "文件夹";
    }

    /**
     * 通过 path 获取到目录下的所有文件，并转换为 FileInfo 对象
     *
     * @param path           目录
     * @param selectFunction 判断选择状态
     * @return FileInfo 集合
     */
    public static List<FileInfo> getFileInfosForPath(String path, Function<FileInfo, Boolean> selectFunction) {
        return getFileInfosForPath(new File(path), selectFunction);
    }

    public static List<FileInfo> getFileInfosForPath(File file, Function<FileInfo, Boolean> selectFunction) {
        List<FileInfo> fileInfos = new ArrayList<>();
        File[] files = file.listFiles();
        if (files == null) {
            return fileInfos;
        }
        for (File child : files) {
            String name = child.getName();
            // 过滤隐藏文件
            if (name.startsWith(".")) continue;
            FileInfo fileInfo = new FileInfo(name, child.getPath(), 0L);
            if (child.isFile()) {
                long length = child.length();
                fileInfo.setLength(length);
                String extension = ando.file.core.FileUtils.INSTANCE.getExtension(name);
                fileInfo.setExtension(extension);
                fileInfo.setFileDesc(toSize(length));
            } else {
                fileInfo.setFileDesc(getFolderChineseName(fileInfo.getUri()));
            }
            if (selectFunction.apply(fileInfo)) {
                fileInfo.setSelected(true);
            }
            fileInfos.add(fileInfo);
        }
        fileInfos.sort(Comparator.comparing(FileInfo::getName));
        return fileInfos;
    }

    /**
     * 选择外部 App 打开文件
     *
     * @param file 文件
     * @return 是否支持打开
     */
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
