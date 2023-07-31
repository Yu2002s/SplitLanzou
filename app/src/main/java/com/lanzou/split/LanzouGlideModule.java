package com.lanzou.split;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.lanzou.split.data.FileInfo;
import com.lanzou.split.utils.ApkLoaderFactory;

import java.io.InputStream;

@GlideModule
public class LanzouGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        super.applyOptions(context, builder);
        Log.d("jdy", "applyOptions");
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // super.registerComponents(context, glide, registry);
        Log.d("jdy", "register");
        registry.prepend(FileInfo.class, InputStream.class, new ApkLoaderFactory(context));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
