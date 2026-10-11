package com.eypa.app.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.eypa.app.api.ApiClient;

import java.io.InputStream;

/** Treat WordPress network-relative URLs as HTTPS rather than local file paths. */
public final class RelativeImageUrlLoader implements ModelLoader<String, InputStream> {
    private final ModelLoader<GlideUrl, InputStream> delegate;

    private RelativeImageUrlLoader(ModelLoader<GlideUrl, InputStream> delegate) { this.delegate = delegate; }

    @Nullable
    @Override public LoadData<InputStream> buildLoadData(@NonNull String model, int width, int height, @NonNull Options options) {
        String source = model.trim();
        String absolute = source.startsWith("//") ? "https:" + source : ApiClient.BASE_URL + source.substring(1);
        return delegate.buildLoadData(new GlideUrl(absolute), width, height, options);
    }

    @Override public boolean handles(@NonNull String model) {
        String source = model.trim();
        return source.startsWith("//") || source.startsWith("/wp-content/uploads/");
    }

    public static final class Factory implements ModelLoaderFactory<String, InputStream> {
        @NonNull @Override public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory factory) {
            return new RelativeImageUrlLoader(factory.build(GlideUrl.class, InputStream.class));
        }
        @Override public void teardown() { }
    }
}
