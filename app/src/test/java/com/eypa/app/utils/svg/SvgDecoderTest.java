package com.eypa.app.utils.svg;

import com.bumptech.glide.load.Options;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SvgDecoderTest {
    @Test public void pngIsNotConsumedBySvgParser() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, '<', 's', 'v', 'g', '>'};
        ByteArrayInputStream stream = new ByteArrayInputStream(png);
        assertFalse(new SvgDecoder().handles(stream, new Options()));
        byte[] remaining = new byte[png.length];
        assertEquals(png.length, stream.read(remaining));
        assertArrayEquals(png, remaining);
    }

    @Test public void svgPrologueCanBeSniffedWithoutConsumingTheBody() throws Exception {
        String svg = "<?xml version=\"1.0\"?><!-- icon --><svg xmlns=\"http://www.w3.org/2000/svg\"/>";
        ByteArrayInputStream stream = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));
        assertTrue(new SvgDecoder().handles(stream, new Options()));
        assertEquals('<', stream.read());
    }

    @Test public void nonRewindableStreamIsLeftUntouched() throws Exception {
        InputStream stream = new FilterInputStream(new ByteArrayInputStream("<svg/>".getBytes(StandardCharsets.UTF_8))) {
            @Override public boolean markSupported() { return false; }
        };
        assertFalse(new SvgDecoder().handles(stream, new Options()));
        assertEquals('<', stream.read());
    }
}
