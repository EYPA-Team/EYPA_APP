package com.eypa.app.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public final class ResourceClient {
    private static ResourceAccess instance;

    private ResourceClient() { }

    public static synchronized ResourceAccess get(Context context) {
        if (instance == null) {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            OkHttpClient transport = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build();
            instance = new ResourceAccess(HttpUrl.get(ApiClient.BASE_URL), () -> prefs.getString("auth_token", ""), transport);
        }
        return instance;
    }

    @androidx.annotation.OptIn(markerClass = UnstableApi.class)
    public static DefaultMediaSourceFactory mediaSourceFactory(Context context) {
        OkHttpDataSource.Factory http = new OkHttpDataSource.Factory(get(context).client());
        return new DefaultMediaSourceFactory(new DefaultDataSource.Factory(context, http));
    }
}
