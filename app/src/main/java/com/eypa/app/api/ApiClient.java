package com.eypa.app.api;

import android.util.Log;

import com.eypa.app.model.ContentItem;
import com.eypa.app.model.Term;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://eqmemory.cn/";
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
                    .sslSocketFactory(createSSLSocketFactory(), new TrustAllCerts())
                    .hostnameVerifier((hostname, session) -> true) // 信任所有主机名 (注意：仅限开发环境)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    private static SSLSocketFactory createSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public static ContentApiService getApiService() {
        return getClient().create(ContentApiService.class);
    }
}
