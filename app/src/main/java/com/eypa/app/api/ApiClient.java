package com.eypa.app.api;

import android.util.Log;

import com.eypa.app.model.ContentItem;
import com.eypa.app.model.Term;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.concurrent.TimeUnit;


import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    public static final String BASE_URL = "https://eqmemory.cn/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
                if (message.startsWith("{") || message.startsWith("[")) {
                    Log.d("NETWORK_JSON", message);
                } else {
                    Log.d("NETWORK", message);
                }
            });
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            // 1. 创建 GsonBuilder 以便自定义 Gson 实例
            GsonBuilder gsonBuilder = new GsonBuilder();

            // 2. 为 Term.class 注册我们的自定义反序列化器
            gsonBuilder.registerTypeAdapter(Term.class, new TermDeserializer());

            gsonBuilder.registerTypeAdapter(
                    new TypeToken<List<ContentItem.VideoEpisode>>(){}.getType(),
                    new VideoEpisodeListDeserializer()
            );

            Gson gson = gsonBuilder.create();

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static ContentApiService getApiService() {
        return getClient().create(ContentApiService.class);
    }
}
