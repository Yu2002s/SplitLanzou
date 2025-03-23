package com.lanzou.cloud.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.lanzou.cloud.LanzouApplication;

/**
 * 改用开源框架
 * com.github.javakam:file.core
 */
@Deprecated
public class UriUtils {

    @SuppressLint("Range")
    public static String getFileName(Uri uri) {
        String fileName = null;
        try {
            Cursor cursor = getCursor(uri);
            fileName = getFileName(cursor);
        } catch (Exception ignored) {
        }
        if (fileName == null) {
            DocumentFile documentFile = DocumentFile.fromSingleUri(LanzouApplication.context, uri);
            if (documentFile != null) {
                fileName = documentFile.getName();
            }
        }
        return fileName == null ? String.valueOf(System.currentTimeMillis()) : fileName;
    }

    @Nullable
    public static String getFileName(@Nullable Cursor cursor) throws Exception {
        if (cursor != null) {
            return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
        }
        return null;
    }

    @SuppressLint("Range")
    @Nullable
    public static Long getFileLength(@Nullable Cursor cursor, Uri uri) {
        Long length = null;
        try {
            if (cursor != null) {
                length = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
            }
        } catch (Exception ignored) {
        }
        if (length == null) {
            DocumentFile documentFile = DocumentFile.fromSingleUri(LanzouApplication.context, uri);
            if (documentFile != null) {
                length = documentFile.length();
            }
        }
        return length;
    }

    @Nullable
    public static Cursor getCursor(Uri uri) {
        ContentResolver contentResolver = LanzouApplication.context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            return cursor;
        }
        return null;
    }

    @Nullable
    public static DocumentFile getDocumentFile(Uri uri) {
        return DocumentFile.fromSingleUri(LanzouApplication.context, uri);
    }

    @SuppressLint("Range")
    @Nullable
    public static String getFilePath(Uri uri) {
        if (uri.getScheme().equals("file")) {
            return uri.getPath();
        }
        String path = null;
        try {
            Cursor cursor = getCursor(uri);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (columnIndex != -1) {
                    path = cursor.getString(columnIndex);
                }
            }
            if (path == null) {
                path = getPath(uri);
            }
        } catch (Exception e) {
        }
        return path;
    }

    @Nullable
    private static String getPath(Uri uri) {
        String authority = uri.getAuthority();
        String[] documentId = DocumentsContract.getDocumentId(uri).split(":");
        String path = null;
        switch (authority) {
            case "com.android.externalstorage.documents":
                path = "/storage/emulated/0/" + documentId[1];
                break;
            case "com.android.providers.media.documents":
                //path = getMediaPath(documentId[0], documentId[1]);
            case "com.android.providers.downloads.documents":
                /*DocumentFile documentFile = DocumentFile.fromSingleUri(LanzouApplication.context, uri);
                Log.d("jdy", documentFile.getName());*/
                if (documentId.length == 1) {
                    path = getDownloadPath(uri);
                } else {
                    if (documentId[0].equals("raw")) {
                        path = documentId[1];
                    } else {
                        path = getMediaPath(documentId[0], documentId[1]);
                    }
                }
                break;
        }
        return path;
    }

    @Nullable
    public static String getMediaPath(String type, String id) {
        Uri contentUri;
        switch (type) {
            case "image":
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                break;
            case "video":
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
            case "audio":
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;
            default:
                contentUri = MediaStore.Files.getContentUri("external");
                break;
        }
        return queryPath(contentUri, id);
    }

    @Nullable
    private static String queryPath(Uri contentUri, String id) {
        ContentResolver contentResolver = LanzouApplication.context.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, null, "_id = ?", new String[]{id}, null);
        if (cursor.moveToFirst()) {
            String s = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            cursor.close();
            return s;
        }
        return null;
    }

    private static String getDownloadPath(Uri uri) {
        /*Uri uri = Uri.parse("content://downloads/my_downloads");
        Uri contentUri = ContentUris.withAppendedId(uri, Long.parseLong(id));*/
        final String id = DocumentsContract.getDocumentId(uri);
        uri = ContentUris.withAppendedId(Uri.parse("content://downloads/all_downloads"), Long.parseLong(id));
        Cursor cursor = LanzouApplication.context.getContentResolver()
                .query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String data = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            cursor.close();
        }
        return null;
    }

    private static String getDataColumn(Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = LanzouApplication.context.getContentResolver()
                    .query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


}
