package com.lanzou.cloud;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.lanzou.cloud.data.FileInfo;
import com.lanzou.cloud.utils.ApkLoaderFactory;

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
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
