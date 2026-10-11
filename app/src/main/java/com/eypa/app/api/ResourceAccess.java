package com.eypa.app.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Website media protocol. Signing secrets never leave WordPress. */
public final class ResourceAccess {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType TEXT = MediaType.get("text/plain; charset=utf-8");
    private static final Pattern SCOPE = Pattern.compile("[a-f0-9]{32}");
    private static final Pattern CONTROLS = Pattern.compile("[\\x00-\\x1f\\x7f]");
    private final HttpUrl apiBase;
    private final Supplier<String> tokenProvider;
    private final OkHttpClient transport, client;
    private final Gson gson = new Gson();
    private volatile Config config;
    private Identity identity;

    public ResourceAccess(HttpUrl apiBase, Supplier<String> tokenProvider, OkHttpClient transport) {
        this.apiBase = apiBase;
        this.tokenProvider = tokenProvider;
        this.transport = transport.newBuilder().followRedirects(false).followSslRedirects(false).build();
        client = transport.newBuilder().cookieJar(CookieJar.NO_COOKIES)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    if (!isCandidate(original.url()) || (!"GET".equals(original.method()) && !"HEAD".equals(original.method()))) {
                        return chain.proceed(original);
                    }
                    Config settings = config();
                    String key = keyOf(original.url(), settings);
                    if (key == null) return chain.proceed(original);
                    Identity owner = identity();
                    Lease lease = owner.lease(key);
                    HttpUrl url = authorize(owner, lease, settings, false);
                    checkIdentity(owner);
                    Response response = chain.proceed(embedded(original, url, settings, owner));
                    // Recover one expired/evicted cookie by rechecking permissions.
                    if (response.code() == 401 || response.code() == 403) {
                        response.close();
                        url = authorize(owner, lease, settings, true);
                        checkIdentity(owner);
                        response = chain.proceed(embedded(original, url, settings, owner));
                    }
                    return response;
                })
                .addNetworkInterceptor(chain -> {
                    Request request = chain.request();
                    HttpUrl protectedOrigin = request.tag(HttpUrl.class);
                    if (protectedOrigin != null && !sameOrigin(request.url(), protectedOrigin)) {
                        request = request.newBuilder().removeHeader("Cookie").removeHeader("Origin")
                                .removeHeader("Referer").removeHeader("Sec-Fetch-Mode")
                                .removeHeader("Sec-Fetch-Site").removeHeader("Sec-Fetch-Dest").build();
                    }
                    return chain.proceed(request);
                }).build();
    }

    public OkHttpClient client() { return client; }

    /** Opaque cache namespace changes with login/logout, without storing tokens in cache keys. */
    public String imageCacheIdentity() { return identity().cacheId; }

    public boolean isCandidate(HttpUrl url) {
        String host = apiBase.host();
        boolean ownHost = url.host().equals(host) || url.host().endsWith("." + host);
        Config current = config;
        if (current != null) ownHost |= sameOrigin(url, current.media) || sameOrigin(url, current.publicBase);
        String path = url.encodedPath();
        return ownHost && (path.startsWith("/uploads/") || path.startsWith("/v3/")
                || path.startsWith("/wp-content/uploads/") || "eu_r2_file".equals(url.queryParameter("action")));
    }

    /** Obtain a fresh download ticket and stream a single POST response. */
    public Response download(String source, int attachmentId) throws IOException {
        Config settings = config();
        Identity owner = identity();
        if (settings.loginRequired && owner.token.isEmpty()) throw new IOException("请先登录后下载附件。");
        String sid = session(owner, settings);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("token", owner.token);
        request.put("purpose", "download");
        request.put("sid", sid);
        request.put("file", attachmentId);
        request.put("url", source);
        Grant grant = grant(owner, request);
        if (grant.ticket == null || grant.ticket.isEmpty()) throw new IOException("下载授权响应不完整。");
        checkIdentity(owner);
        // No automatic replay: interruption consumes a quota slot. A new user
        // action must get a new ticket rather than repeat the consumed POST.
        Response response = owner.control.newCall(edge(settings.download.resolve("/download"), settings)
                .post(new FormBody.Builder().add("ticket", grant.ticket).build()).build()).execute();
        if (!response.isSuccessful()) {
            try (Response failed = response) { throw error(failed); }
        }
        return response;
    }

    private synchronized Config config() throws IOException {
        if (config != null) return config;
        try (Response response = transport.newCall(new Request.Builder()
                .url(apiBase.resolve("eu-json/app/v1/media/config")).build()).execute()) {
            if (!response.isSuccessful()) throw error(response);
            Config candidate = parse(response, Config.class);
            candidate.media = origin(candidate.origin);
            candidate.download = origin(candidate.downloadOrigin);
            candidate.site = origin(candidate.siteOrigin);
            candidate.publicBase = candidate.publicUrl == null ? null : HttpUrl.parse(candidate.publicUrl);
            if ((candidate.protocol != 3 && candidate.protocol != 2) || !"uploads".equals(candidate.prefix)
                    || candidate.publicBase == null) throw new IOException("媒体鉴权配置无效。");
            config = candidate;
            return candidate;
        }
    }

    private HttpUrl origin(String value) throws IOException {
        HttpUrl result = value == null ? null : HttpUrl.parse(value);
        boolean localTest = apiBase.host().equals("localhost") || apiBase.host().equals("127.0.0.1");
        if (result == null || (!result.isHttps() && !localTest) || !result.encodedPath().equals("/")
                || result.query() != null || result.fragment() != null || !result.username().isEmpty()
                || !result.password().isEmpty()) throw new IOException("媒体域名配置无效。");
        return result;
    }

    private String token() {
        String value = tokenProvider.get();
        return value == null ? "" : value;
    }

    private synchronized Identity identity() {
        String token = token();
        // Every identity owns a separate cookie jar and scoped URLs. Concurrent
        // requests from a previous account cannot install the new account's cookie.
        if (identity == null || !Objects.equals(identity.token, token)) identity = new Identity(token);
        return identity;
    }

    private void checkIdentity(Identity owner) throws IOException {
        if (!owner.token.equals(token())) throw new IOException("登录状态已改变，请重试。");
    }

    private String keyOf(HttpUrl url, Config settings) throws IOException {
        // Upgrade historical HTTP references only when identifying our HTTPS CDN.
        if (url.scheme().equals("http") && url.port() == 80) url = url.newBuilder().scheme("https").build();
        List<String> segments = url.pathSegments(); // Decode once; '+' remains literal.
        List<String> key = null;
        if (sameOrigin(url, settings.media) || sameOrigin(url, settings.publicBase)) {
            if (segments.size() > 3 && segments.get(0).equals("v3") && SCOPE.matcher(segments.get(1)).matches()) {
                key = segments.subList(2, segments.size());
            } else if (!segments.isEmpty() && segments.get(0).equals(settings.prefix)) {
                key = segments;
            }
        } else if (sameOrigin(url, apiBase) || sameOrigin(url, settings.site)) {
            if (url.encodedPath().startsWith("/wp-content/uploads/")) {
                key = new ArrayList<>(); key.add(settings.prefix); key.addAll(segments.subList(2, segments.size()));
            } else if ("eu_r2_file".equals(url.queryParameter("action"))) {
                String file = url.queryParameter("file");
                if (file != null) key = Arrays.asList(file.split("/", -1));
            }
        }
        if (key == null) return null;
        for (String part : key) {
            if (part.isEmpty() || part.equals(".") || part.equals("..") || part.indexOf('/') >= 0
                    || part.indexOf('\\') >= 0 || CONTROLS.matcher(part).find()) throw new IOException("无效的媒体路径。");
        }
        String value = String.join("/", key);
        if (!value.startsWith(settings.prefix + "/") || value.getBytes(StandardCharsets.UTF_8).length > 1024) {
            throw new IOException("无效的媒体路径。");
        }
        return value;
    }

    private HttpUrl authorize(Identity owner, Lease lease, Config settings, boolean force) throws IOException {
        synchronized (lease) {
            String path = settings.protocol == 3 ? "v3/" + lease.scope + "/" + lease.key : lease.key;
            HttpUrl target = settings.media.newBuilder().addPathSegments(path).build();
            long now = System.nanoTime();
            if (lease.deniedUntil > now) throw new IOException("没有访问此资源的权限，或文件已失效。");
            if (!force && lease.until > now + TimeUnit.SECONDS.toNanos(40)
                    && !owner.cookies.loadForRequest(target).isEmpty()) return target;
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("token", owner.token);
            request.put("keys", new ArrayList<>(Collections.singletonList(path)));
            request.put("purpose", "preview");
            if (settings.protocol == 2) request.put("sid", session(owner, settings));
            Grant result = grant(owner, request);
            if (result.denied != null && result.denied.contains(path)) {
                lease.until = 0; lease.deniedUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                throw new IOException("没有访问此资源的权限，或文件已失效。");
            }
            long expiry;
            if (settings.protocol == 3) {
                Batch batch = result.batches == null ? null : result.batches.stream()
                        .filter(item -> lease.scope.equals(item.scope) && item.keys != null && item.keys.contains(path))
                        .findFirst().orElse(null);
                if (batch == null || batch.ticket == null) throw new IOException("预览授权响应不完整。");
                checkIdentity(owner);
                try (Response response = owner.control.newCall(edge(settings.media.resolve("/v3/" + lease.scope + "/authorize"), settings)
                        .post(RequestBody.create(batch.ticket, TEXT)).build()).execute()) {
                    if (!response.isSuccessful()) throw error(response);
                }
                expiry = batch.expires;
            } else {
                if (result.keys == null || !result.keys.contains(path) || result.ticket == null) throw new IOException("媒体授权响应不完整。");
                JsonObject body = new JsonObject(); body.addProperty("ticket", result.ticket);
                try (Response response = owner.control.newCall(edge(settings.media.resolve("/grant"), settings)
                        .post(RequestBody.create(gson.toJson(body), JSON)).build()).execute()) {
                    if (!response.isSuccessful()) throw error(response);
                }
                expiry = result.expires;
            }
            checkIdentity(owner);
            long remaining = expiry - result.serverTime;
            if (result.serverTime <= 0 || remaining <= 0 || owner.cookies.loadForRequest(target).isEmpty()) {
                throw new IOException("媒体授权已过期或未能建立凭证，请重试。");
            }
            lease.until = System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.min(remaining, 120));
            return target;
        }
    }

    private synchronized String session(Identity owner, Config settings) throws IOException {
        try (Response response = owner.control.newCall(edge(settings.download.resolve("/session"), settings)
                .post(RequestBody.create("{}", JSON)).build()).execute()) {
            if (!response.isSuccessful()) throw error(response);
            JsonObject body = parse(response, JsonObject.class);
            String sid = body.has("sid") ? body.get("sid").getAsString() : "";
            if (!sid.matches("[a-f0-9]{64}")) throw new IOException("媒体会话响应无效。");
            return sid;
        }
    }

    private Grant grant(Identity owner, Map<String, Object> data) throws IOException {
        checkIdentity(owner);
        try (Response response = transport.newCall(new Request.Builder().url(apiBase.resolve("eu-json/app/v1/media/grant"))
                .post(RequestBody.create(gson.toJson(data), JSON)).build()).execute()) {
            if (!response.isSuccessful()) throw error(response);
            return parse(response, Grant.class);
        }
    }

    private Request.Builder edge(HttpUrl url, Config settings) {
        return new Request.Builder().url(url).header("Origin", stripSlash(settings.site)).header("Referer", settings.site.toString());
    }

    private Request embedded(Request original, HttpUrl url, Config settings, Identity owner) {
        String cookie = String.join("; ", owner.cookies.loadForRequest(url).stream()
                .map(value -> value.name() + "=" + value.value()).toArray(String[]::new));
        return original.newBuilder().url(url).tag(HttpUrl.class, settings.media).removeHeader("Authorization")
                .header("Cookie", cookie).header("Origin", stripSlash(settings.site)).header("Referer", settings.site.toString())
                .header("Sec-Fetch-Mode", "cors").header("Sec-Fetch-Site", "same-site").header("Sec-Fetch-Dest", "empty").build();
    }

    private <T> T parse(Response response, Class<T> type) throws IOException {
        try {
            if (response.body() == null) throw new IOException("媒体接口返回空响应。");
            T value = gson.fromJson(response.body().string(), type);
            if (value == null) throw new IOException("媒体接口返回空响应。");
            return value;
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("媒体接口返回无效数据。", e);
        }
    }

    private IOException error(Response response) {
        String message = response.code() == 429 ? "下载次数或访问额度已用尽，请稍后重试。" : "媒体访问失败 (" + response.code() + ")";
        try {
            JsonObject body = parse(response, JsonObject.class);
            if (body.has("message")) message = body.get("message").getAsString();
        } catch (Exception ignored) { }
        return new IOException(message);
    }

    private static boolean sameOrigin(HttpUrl first, HttpUrl second) {
        return second != null && first.scheme().equals(second.scheme()) && first.host().equals(second.host()) && first.port() == second.port();
    }

    private static String stripSlash(HttpUrl url) { return url.toString().substring(0, url.toString().length() - 1); }

    private final class Identity {
        final String token;
        final String cacheId = UUID.randomUUID().toString();
        final MemoryCookies cookies = new MemoryCookies();
        final OkHttpClient control = transport.newBuilder().cookieJar(cookies).retryOnConnectionFailure(false).build();
        final Map<String, Lease> leases = new LinkedHashMap<>();
        Identity(String token) { this.token = token; }
        synchronized Lease lease(String key) {
            Lease lease = leases.get(key);
            if (lease == null) {
                lease = new Lease(key); leases.put(key, lease);
                while (leases.size() > 512) leases.remove(leases.keySet().iterator().next());
            }
            return lease;
        }
    }

    private static final class Lease {
        final String key, scope = UUID.randomUUID().toString().replace("-", "");
        long until, deniedUntil;
        Lease(String key) { this.key = key; }
    }

    private static final class MemoryCookies implements CookieJar {
        private final List<Cookie> values = new ArrayList<>();
        @Override public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            values.removeIf(cookie -> cookie.expiresAt() <= System.currentTimeMillis());
            for (Cookie next : cookies) {
                values.removeIf(old -> old.name().equals(next.name()) && old.domain().equals(next.domain()) && old.path().equals(next.path()));
                if (next.expiresAt() > System.currentTimeMillis()) values.add(next);
            }
            while (values.size() > 512) values.remove(0);
        }
        @Override public synchronized List<Cookie> loadForRequest(HttpUrl url) {
            values.removeIf(cookie -> cookie.expiresAt() <= System.currentTimeMillis());
            List<Cookie> matching = new ArrayList<>();
            for (Cookie cookie : values) if (cookie.matches(url)) matching.add(cookie);
            return matching;
        }
    }

    private static final class Config {
        int protocol;
        String origin, prefix;
        @SerializedName("download_origin") String downloadOrigin;
        @SerializedName("site_origin") String siteOrigin;
        @SerializedName("public_url") String publicUrl;
        @SerializedName("login_required") boolean loginRequired;
        transient HttpUrl media, download, site, publicBase;
    }
    private static final class Grant {
        String ticket;
        List<String> keys, denied;
        List<Batch> batches;
        long expires;
        @SerializedName("server_time") long serverTime;
    }
    private static final class Batch {
        String scope, ticket;
        List<String> keys;
        long expires;
    }
}
