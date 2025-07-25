package com.lanzou.cloud.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.lanzou.cloud.model.FileInfoModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ApkLoaderFactory2 implements ModelLoaderFactory<FileInfoModel, InputStream> {

    private final Context context;

    public ApkLoaderFactory2(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<FileInfoModel, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new ApkIconLoader(context);
    }

    @Override
    public void teardown() {

    }

    static class ApkIconLoader implements ModelLoader<FileInfoModel, InputStream> {

        private final Context context;

        public ApkIconLoader(Context context) {
            this.context = context;
        }

        @Nullable
        @Override
        public LoadData<InputStream> buildLoadData(@NonNull FileInfoModel fileInfo, int width, int height, @NonNull Options options) {
            Object key;
            if (!TextUtils.isEmpty(fileInfo.getPkgName())) {
                key = fileInfo.getPkgName();
            } else if (!TextUtils.isEmpty(fileInfo.getPath())) {
                key = fileInfo.getPath();
            } else {
                key = fileInfo.hashCode();
            }
            return new LoadData<>(new ObjectKey(key), new ApkIconFetcher(context, fileInfo));

        }

        @Override
        public boolean handles(@NonNull FileInfoModel fileInfo) {
            return true;
        }
    }

    static class ApkIconFetcher implements DataFetcher<InputStream> {

        private final Context context;
        private final FileInfoModel fileInfo;

        public ApkIconFetcher(Context context, FileInfoModel fileInfo) {
            this.context = context;
            this.fileInfo = fileInfo;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            ApplicationInfo applicationInfo;
            try {
                PackageManager packageManager = context.getPackageManager();
                Drawable drawable;
                if (fileInfo.getPkgName() == null) {
                    applicationInfo = packageManager.getPackageArchiveInfo(fileInfo.getPath(), 0).applicationInfo;
                    applicationInfo.sourceDir = fileInfo.getPath();
                    applicationInfo.publicSourceDir = fileInfo.getPath();
                    drawable = applicationInfo.loadIcon(packageManager);
                } else {
                    applicationInfo = packageManager.getApplicationInfo(fileInfo.getPkgName(), 0);
                    drawable = packageManager.getApplicationIcon(applicationInfo); //xxx根据自己的情况获取drawable
                }

                InputStream inputStream = bitmap2InputStream(drawable2Bitmap(drawable));
                callback.onDataReady(inputStream);
            } catch (Exception e) {
                callback.onLoadFailed(e);
            }
        }

        private InputStream bitmap2InputStream(Bitmap bm) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
            return new ByteArrayInputStream(bos.toByteArray());
        }

        private Bitmap drawable2Bitmap(Drawable drawable) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }

        @Override
        public void cleanup() {

        }

        @Override
        public void cancel() {

        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}
