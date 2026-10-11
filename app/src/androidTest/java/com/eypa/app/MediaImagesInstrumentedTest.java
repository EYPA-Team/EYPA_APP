package com.eypa.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.signature.ObjectKey;
import com.eypa.app.ui.home.AuthorProfileActivity;
import com.eypa.app.ui.widget.ZoomableImageView;
import com.eypa.app.utils.MediaImages;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MediaImagesInstrumentedTest {
    @Test public void recycledImageUsesCacheInsteadOfFetchingAgain() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicInteger fetches = new AtomicInteger();
        Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.GREEN);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        byte[] png = bytes.toByteArray();
        bitmap.recycle();
        Glide.get(context).getRegistry().prepend(CacheFixture.class, InputStream.class, new ModelLoaderFactory<CacheFixture, InputStream>() {
            @NonNull @Override public ModelLoader<CacheFixture, InputStream> build(@NonNull MultiModelLoaderFactory factory) {
                return new ModelLoader<CacheFixture, InputStream>() {
                    @Override public LoadData<InputStream> buildLoadData(@NonNull CacheFixture model, int width, int height, @NonNull Options options) {
                        return new LoadData<>(new ObjectKey(model.id), new DataFetcher<InputStream>() {
                            @Override public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
                                fetches.incrementAndGet(); callback.onDataReady(new ByteArrayInputStream(png));
                            }
                            @Override public void cleanup() { }
                            @Override public void cancel() { }
                            @NonNull @Override public Class<InputStream> getDataClass() { return InputStream.class; }
                            @NonNull @Override public DataSource getDataSource() { return DataSource.REMOTE; }
                        });
                    }
                    @Override public boolean handles(@NonNull CacheFixture model) { return true; }
                };
            }
            @Override public void teardown() { }
        });
        CacheFixture image = new CacheFixture();
        FutureTarget<Bitmap> first = MediaImages.with(context).asBitmap().load(image).submit(24, 24);
        assertEquals(Color.GREEN, first.get(10, TimeUnit.SECONDS).getPixel(12, 12));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> MediaImages.with(context).clear(first));
        FutureTarget<Bitmap> second = MediaImages.with(context).asBitmap().load(image).submit(24, 24);
        assertEquals(Color.GREEN, second.get(10, TimeUnit.SECONDS).getPixel(12, 12));
        assertEquals("A recycled row must reuse the image", 1, fetches.get());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> MediaImages.with(context).clear(second));
    }

    @Test public void lateAvatarRefitsAfterTransparentPlaceholder() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ZoomableImageView view = new ZoomableImageView(context);
            int size = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
            view.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            view.measure(size, size); view.layout(0, 0, 100, 100);
            Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            bitmap.setDensity(context.getResources().getDisplayMetrics().densityDpi);
            bitmap.eraseColor(Color.BLUE);
            view.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
            view.measure(size, size); view.layout(0, 0, 100, 100);
            float[] matrix = new float[9]; view.getImageMatrix().getValues(matrix);
            assertEquals(0.5f, matrix[Matrix.MSCALE_X], 0.001f);
            Bitmap rendered = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(rendered));
            assertEquals(Color.BLUE, rendered.getPixel(50, 50));
        });
    }

    /** Opt-in smoke check against the actual API; the offline tests above always run. */
    @Test public void liveAuthorAvatarAndHeroRender() throws Exception {
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("eypa_live_images")));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, AuthorProfileActivity.class).putExtra("extra_user_id", 1);
        try (ActivityScenario<AuthorProfileActivity> scenario = ActivityScenario.launch(intent)) {
            AtomicBoolean loaded = new AtomicBoolean();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
            while (!loaded.get() && System.nanoTime() < deadline) {
                scenario.onActivity(activity -> {
                    ImageView avatar = activity.findViewById(R.id.iv_avatar);
                    ImageView hero = activity.findViewById(R.id.iv_cover);
                    loaded.set(avatar.getDrawable() instanceof BitmapDrawable && hero.getDrawable() instanceof BitmapDrawable);
                });
                if (!loaded.get()) Thread.sleep(100);
            }
            assertTrue("Actual profile avatar and hero must decode", loaded.get());
            Thread.sleep(200);
            Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            File directory = context.getExternalFilesDir("test-artifacts");
            assertNotNull(directory);
            if (!directory.isDirectory()) assertTrue(directory.mkdirs());
            try (FileOutputStream output = new FileOutputStream(new File(directory, "profile-after.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
        }
    }

    private static final class CacheFixture { final String id = UUID.randomUUID().toString(); }
}
