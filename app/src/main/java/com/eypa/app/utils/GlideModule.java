package com.eypa.app.utils;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;
import com.eypa.app.utils.svg.SvgDecoder;
import com.eypa.app.utils.svg.SvgDrawableTranscoder;
import com.eypa.app.api.ResourceClient;
import java.io.InputStream;

@com.bumptech.glide.annotation.GlideModule
public class GlideModule extends AppGlideModule {

  @Override
  public void registerComponents(
      @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    registry
        .replace(GlideUrl.class, InputStream.class,
            new OkHttpUrlLoader.Factory(ResourceClient.get(context).client()))
        .prepend(String.class, InputStream.class, new RelativeImageUrlLoader.Factory())
        .register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
        .append(InputStream.class, SVG.class, new SvgDecoder());
  }

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
