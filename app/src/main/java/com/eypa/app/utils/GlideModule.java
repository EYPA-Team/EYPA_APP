package com.eypa.app.utils;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;
import com.eypa.app.utils.svg.SvgDecoder;
import com.eypa.app.utils.svg.SvgDrawableTranscoder;
import java.io.InputStream;

@com.bumptech.glide.annotation.GlideModule
public class GlideModule extends AppGlideModule {

  @Override
  public void registerComponents(
      @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    registry
        .register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
        .append(InputStream.class, SVG.class, new SvgDecoder());
  }

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
