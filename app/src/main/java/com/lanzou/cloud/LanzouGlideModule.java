package com.lanzou.cloud;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.lanzou.cloud.data.FileInfo;
import com.lanzou.cloud.model.FileInfoModel;
import com.lanzou.cloud.utils.ApkLoaderFactory;
import com.lanzou.cloud.utils.ApkLoaderFactory2;

import java.io.InputStream;

@GlideModule
public class LanzouGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        super.applyOptions(context, builder);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // super.registerComponents(context, glide, registry);
        registry.prepend(FileInfo.class, InputStream.class, new ApkLoaderFactory(context));
        registry.prepend(FileInfoModel.class, InputStream.class, new ApkLoaderFactory2(context));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
