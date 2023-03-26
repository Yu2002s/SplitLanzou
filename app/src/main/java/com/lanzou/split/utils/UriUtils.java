package com.lanzou.split.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.lanzou.split.LanzouApplication;

import java.util.Arrays;

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
    public static Cursor getCursor(Uri uri) throws Exception {
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
            Log.e("jdy", e.toString());
        }
        return path;
    }

    @Nullable
    private static String getPath(Uri uri) {
        String authority = uri.getAuthority();
        Log.d("jdy", "uri: " + uri);
        Log.d("jdy", "authority: " + authority);
        String[] documentId = DocumentsContract.getDocumentId(uri).split(":");
        Log.d("jdy", "documentId: " + Arrays.toString(documentId));
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
                path = getMediaPath(documentId[0], documentId[1]);
                break;
        }
        return path;
    }

    @Nullable
    public static String getMediaPath(String type, String id) {
        ContentResolver contentResolver = LanzouApplication.context.getContentResolver();
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
        Cursor cursor = contentResolver.query(contentUri, null, "_id = ?", new String[]{id}, null);
        if (cursor.moveToFirst()) {
            String s = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            cursor.close();
            return s;
        }
        return null;
    }

}
