package com.eypa.app.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ResourceClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/** Streams the download gate directly to storage, without buffering the file. */
public final class ResourceDownloads {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ResourceDownloads() { }

    public static boolean isManaged(Context context, String source) {
        HttpUrl url = HttpUrl.get(ApiClient.BASE_URL).resolve(source);
        return url != null && (ResourceClient.get(context).isCandidate(url) || attachmentId(url) > 0);
    }

    private static int attachmentId(HttpUrl url) {
        HttpUrl site = HttpUrl.get(ApiClient.BASE_URL);
        if (!url.host().equals(site.host())) return 0;
        String file = "download_file".equals(url.queryParameter("action")) ? url.queryParameter("file") : url.queryParameter("download");
        try { return Math.max(0, Integer.parseInt(file)); }
        catch (Exception ignored) { return 0; }
    }

    public static void save(Context context, String source, String suggestedName, boolean image, Runnable finished) {
        Context app = context.getApplicationContext();
        Toast.makeText(app, image ? "正在保存图片…" : "正在下载附件…", Toast.LENGTH_SHORT).show();
        EXECUTOR.execute(() -> {
            String message;
            try {
                HttpUrl url = HttpUrl.get(ApiClient.BASE_URL).resolve(source);
                if (url == null) throw new IOException("资源链接无效。");
                int attachment = attachmentId(url);
                boolean managed = ResourceClient.get(app).isCandidate(url) || attachment > 0;
                try (Response response = managed ? ResourceClient.get(app).download(url.toString(), attachment)
                        : ResourceClient.get(app).client().newCall(new Request.Builder().url(url).build()).execute()) {
                    if (!response.isSuccessful() || response.body() == null) throw new IOException("下载失败 (" + response.code() + ")");
                    store(app, response, url, suggestedName, image);
                }
                message = image ? "图片已保存至相册" : Build.VERSION.SDK_INT >= 29 ? "附件已保存至下载目录/EYPA" : "附件已保存至应用下载目录";
            } catch (Exception error) {
                message = error.getMessage() == null ? "资源保存失败，请重试。" : error.getMessage();
            }
            String result = message;
            MAIN.post(() -> {
                Toast.makeText(app, result, Toast.LENGTH_LONG).show();
                if (finished != null) finished.run();
            });
        });
    }

    private static void store(Context context, Response response, HttpUrl url, String suggestedName, boolean image) throws IOException {
        String name = filename(response.header("Content-Disposition"), suggestedName, url);
        String mime = response.body().contentType() == null ? "application/octet-stream" : response.body().contentType().toString().split(";")[0];
        if (mime.equals("application/octet-stream")) {
            String inferred = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension(name));
            if (inferred != null) mime = inferred;
        }
        if (image && !mime.startsWith("image/")) throw new IOException("此资源不是可保存的图片。");
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, (image ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS) + "/EYPA");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = context.getContentResolver().insert(image ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("无法创建下载文件。");
            try {
                try (InputStream input = response.body().byteStream(); OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                    copy(input, output);
                }
                values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
            } catch (Exception error) {
                context.getContentResolver().delete(uri, null, null);
                throw error;
            }
        } else {
            File directory = image ? new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EYPA")
                    : context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) throw new IOException("无法访问保存目录。");
            File destination = new File(directory, System.currentTimeMillis() + "_" + name);
            try (InputStream input = response.body().byteStream(); OutputStream output = new FileOutputStream(destination)) {
                copy(input, output);
            } catch (Exception error) {
                destination.delete();
                throw error;
            }
            if (image) android.media.MediaScannerConnection.scanFile(context, new String[]{destination.getAbsolutePath()}, new String[]{mime}, null);
        }
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        if (output == null) throw new IOException("无法写入下载文件。");
        byte[] buffer = new byte[64 * 1024];
        int length;
        while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
    }

    private static String filename(String disposition, String suggested, HttpUrl url) {
        String name = suggested;
        if (disposition != null) {
            Matcher encoded = Pattern.compile("filename\\*\\s*=\\s*UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE).matcher(disposition);
            Matcher plain = Pattern.compile("filename\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(disposition);
            if (encoded.find()) name = Uri.decode(encoded.group(1));
            else if (plain.find()) name = plain.group(1);
        }
        if (name == null || name.trim().isEmpty()) name = url.pathSegments().get(url.pathSegments().size() - 1);
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = "resource_" + System.currentTimeMillis();
        return name.length() > 180 ? name.substring(name.length() - 180) : name;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }
}
