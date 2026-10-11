package com.eypa.app.utils.svg;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SvgDecoder implements ResourceDecoder<InputStream, SVG> {

  @Override
  public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
    if (!source.markSupported()) return false;
    // SVG's parser consumes/closes its input. Never offer PNG/JPEG/GIF bodies to
    // it, or later bitmap decoders cannot rewind the already consumed stream.
    source.mark(4096);
    byte[] prefix = new byte[4096];
    int count = 0;
    try {
      while (count < prefix.length) {
        int read = source.read(prefix, count, prefix.length - count);
        if (read <= 0) break;
        count += read;
      }
      String header = new String(prefix, 0, count, StandardCharsets.UTF_8).trim();
      if (header.startsWith("\uFEFF")) header = header.substring(1).trim();
      return header.startsWith("<") && header.contains("<svg");
    } finally {
      source.reset();
    }
  }

  public Resource<SVG> decode(
      @NonNull InputStream source, int width, int height, @NonNull Options options)
      throws IOException {
    try {
      SVG svg = SVG.getFromInputStream(source);
      if (width != -1 && height != -1) {
          svg.setDocumentWidth(width);
          svg.setDocumentHeight(height);
      }
      return new SimpleResource<>(svg);
    } catch (SVGParseException ex) {
      throw new IOException("SVG加载失败", ex);
    }
  }
}
