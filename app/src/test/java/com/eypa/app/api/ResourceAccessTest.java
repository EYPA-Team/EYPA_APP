package com.eypa.app.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

public class ResourceAccessTest {
    private final Gson gson = new Gson();
    private MockWebServer api, media;
    private ResourceAccess access;
    private final AtomicReference<String> token = new AtomicReference<>("a".repeat(32));
    private final AtomicInteger grants = new AtomicInteger(), reads = new AtomicInteger(), installs = new AtomicInteger(), downloads = new AtomicInteger();
    private final Map<String, String> tickets = new ConcurrentHashMap<>();
    private final AtomicReference<JsonObject> lastGrant = new AtomicReference<>();
    private final AtomicReference<RecordedRequest> lastRead = new AtomicReference<>();
    private int protocol = 3, lifetime = 120;
    private boolean denied, expiredCookie, rejectRead, quota;

    @Before public void setup() throws IOException {
        api = new MockWebServer(); media = new MockWebServer();
        api.start(java.net.InetAddress.getByName("127.0.0.1"), 0);
        media.start(java.net.InetAddress.getByName("127.0.0.1"), 0);
        api.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath().equals("/eu-json/app/v1/media/config")) {
                    Map<String, Object> config = new LinkedHashMap<>();
                    config.put("protocol", protocol); config.put("origin", origin(media)); config.put("download_origin", origin(media));
                    config.put("site_origin", origin(api)); config.put("public_url", origin(media) + "/uploads");
                    config.put("prefix", "uploads"); config.put("login_required", true);
                    return json(config);
                }
                if (request.getPath().equals("/eu-json/app/v1/media/grant")) {
                    JsonObject body = gson.fromJson(request.getBody().readUtf8(), JsonObject.class);
                    lastGrant.set(body); int revision = grants.incrementAndGet();
                    if (body.get("token").getAsString().equals("expired")) return json(Collections.singletonMap("message", "登录过期")).setResponseCode(401);
                    if (body.get("purpose").getAsString().equals("download")) return json(Collections.singletonMap("ticket", "download-" + revision));
                    String path = body.getAsJsonArray("keys").get(0).getAsString();
                    Map<String, Object> result = new LinkedHashMap<>();
                    long now = System.currentTimeMillis() / 1000;
                    result.put("server_time", now); result.put("denied", denied ? Collections.singletonList(path) : new java.util.ArrayList<>());
                    if (protocol == 3) {
                        String scope = path.split("/")[1], ticket = "p3.ticket-" + revision;
                        tickets.put(scope, ticket);
                        Map<String, Object> batch = new LinkedHashMap<>();
                        batch.put("scope", scope); batch.put("ticket", ticket); batch.put("keys", Collections.singletonList(path)); batch.put("expires", now + lifetime);
                        result.put("batches", denied ? new java.util.ArrayList<>() : Collections.singletonList(batch));
                    } else {
                        result.put("keys", denied ? new java.util.ArrayList<>() : Collections.singletonList(path));
                        result.put("ticket", "s2.preview"); result.put("expires", now + lifetime);
                    }
                    return json(result);
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        media.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.endsWith("/authorize")) {
                    installs.incrementAndGet();
                    String scope = path.split("/")[2];
                    if (!request.getMethod().equals("POST") || !request.getBody().readUtf8().equals(tickets.get(scope))
                            || !request.getHeader("Content-Type").startsWith("text/plain")) return new MockResponse().setResponseCode(403);
                    return json(new LinkedHashMap<>()).addHeader("Set-Cookie", "preview=" + tickets.get(scope) + "; Path=/v3/" + scope + "/; Max-Age=" + (expiredCookie ? 0 : 120));
                }
                if (path.equals("/session")) return json(Collections.singletonMap("sid", "b".repeat(64)))
                        .addHeader("Set-Cookie", "session=opaque; Path=/; Max-Age=1800");
                if (path.equals("/grant")) return json(new LinkedHashMap<>());
                if (path.equals("/download")) {
                    downloads.incrementAndGet();
                    if (!request.getMethod().equals("POST") || !request.getBody().readUtf8().startsWith("ticket=download-")) return new MockResponse().setResponseCode(403);
                    if (quota) return json(Collections.singletonMap("error", "daily_limit")).setResponseCode(429);
                    return new MockResponse().setBody("download-body").addHeader("Content-Type", "image/jpeg");
                }
                lastRead.set(request); int attempt = reads.incrementAndGet();
                if (rejectRead && attempt == 1) return new MockResponse().setResponseCode(403);
                if (request.getHeader("Cookie") == null || !origin(api).equals(request.getHeader("Origin"))) return new MockResponse().setResponseCode(403);
                if (protocol == 3 && !request.getHeader("Cookie").equals("preview=" + tickets.get(path.split("/")[2]))) return new MockResponse().setResponseCode(403);
                return new MockResponse().setResponseCode(request.getHeader("Range") == null ? 200 : 206).setBody("media-body");
            }
        });
        access = new ResourceAccess(url(api, "/"), token::get, new OkHttpClient());
    }

    @After public void teardown() throws IOException { api.shutdown(); media.shutdown(); }

    private okhttp3.HttpUrl url(MockWebServer server, String path) { return server.url(path).newBuilder().host("127.0.0.1").build(); }
    private String origin(MockWebServer server) { String url = url(server, "/").toString(); return url.substring(0, url.length() - 1); }
    private MockResponse json(Object value) { return new MockResponse().setHeader("Content-Type", "application/json").setBody(gson.toJson(value)); }
    private Request image() { return new Request.Builder().url(url(media, "/uploads/image.jpg")).build(); }
    private void load(Request request) throws IOException { try (Response response = access.client().newCall(request).execute()) { assertTrue(response.isSuccessful()); assertEquals("media-body", response.body().string()); } }

    @Test public void installsPreviewCookieAndPreservesRangeWithoutLeakingAppToken() throws IOException {
        load(new Request.Builder().url(url(media, "/uploads/中文+100%25 clip.mp4?old=1")).header("Range", "bytes=10-19").build());
        assertEquals(token.get(), lastGrant.get().get("token").getAsString());
        assertTrue(lastGrant.get().getAsJsonArray("keys").get(0).getAsString().endsWith("uploads/中文+100% clip.mp4"));
        RecordedRequest read = lastRead.get();
        assertNull(read.getRequestUrl().query());
        assertTrue(read.getPath().contains("%E4%B8%AD%E6%96%87+100%25%20clip.mp4"));
        assertEquals("bytes=10-19", read.getHeader("Range"));
        assertEquals("cors", read.getHeader("Sec-Fetch-Mode"));
        assertEquals("same-site", read.getHeader("Sec-Fetch-Site"));
        assertEquals("empty", read.getHeader("Sec-Fetch-Dest"));
        assertNull(read.getHeader("Authorization"));
        assertFalse(read.getHeaders().toString().contains(token.get()));
        assertEquals(1, installs.get());
    }

    @Test public void sharesLeaseForRawAndPreviouslyScopedUrls() throws IOException {
        load(image());
        String path = lastRead.get().getPath();
        load(new Request.Builder().url(url(media, path)).build());
        assertEquals(1, grants.get()); assertEquals(2, reads.get());
    }

    @Test public void renewsBeforeShortLeaseExpires() throws IOException {
        lifetime = 20; load(image()); load(image()); assertEquals(2, grants.get());
    }

    @Test public void parallelRequestsForSameResourceShareOneGrant() throws Exception {
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> first = workers.submit(() -> { load(image()); return null; });
            java.util.concurrent.Future<?> second = workers.submit(() -> { load(image()); return null; });
            first.get(5, java.util.concurrent.TimeUnit.SECONDS); second.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1, grants.get()); assertEquals(2, reads.get());
        } finally { workers.shutdownNow(); }
    }

    @Test public void parallelResourcesKeepTheirScopedCookiesSeparate() throws Exception {
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> first = workers.submit(() -> { load(image()); return null; });
            java.util.concurrent.Future<?> second = workers.submit(() -> { load(new Request.Builder().url(url(media, "/uploads/second.jpg")).build()); return null; });
            first.get(5, java.util.concurrent.TimeUnit.SECONDS); second.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(2, grants.get()); assertEquals(2, reads.get()); assertEquals(2, installs.get());
        } finally { workers.shutdownNow(); }
    }

    @Test public void retriesRejectedReadOnceWithFreshPermissions() throws IOException {
        rejectRead = true; load(image()); assertEquals(2, reads.get()); assertEquals(2, grants.get());
    }

    @Test public void deniedPermissionNeverRequestsTheFile() throws IOException {
        denied = true;
        assertThrows(IOException.class, () -> load(image()));
        assertThrows(IOException.class, () -> load(image()));
        assertEquals(0, reads.get()); assertEquals(1, grants.get());
    }

    @Test public void invalidTokenNeverBecomesGuest() {
        token.set("expired");
        IOException error = assertThrows(IOException.class, () -> load(image()));
        assertEquals("登录过期", error.getMessage()); assertEquals(0, reads.get());
    }

    @Test public void logoutCreatesNewScopeAndCookies() throws IOException {
        load(image()); String oldPath = lastRead.get().getPath(); String oldCookie = lastRead.get().getHeader("Cookie");
        token.set(""); load(image());
        assertEquals("", lastGrant.get().get("token").getAsString());
        assertNotEquals(oldPath, lastRead.get().getPath()); assertNotEquals(oldCookie, lastRead.get().getHeader("Cookie"));
    }

    @Test public void imageCacheNamespaceIsStableUntilIdentityChanges() {
        String loggedIn = access.imageCacheIdentity();
        assertEquals(loggedIn, access.imageCacheIdentity());
        assertFalse(loggedIn.contains(token.get()));
        token.set("");
        String guest = access.imageCacheIdentity();
        assertNotEquals(loggedIn, guest);
        assertEquals(guest, access.imageCacheIdentity());
        token.set("c".repeat(32));
        assertNotEquals(guest, access.imageCacheIdentity());
        assertEquals(0, api.getRequestCount());
    }

    @Test public void refusesMissingOrExpiredCookie() {
        expiredCookie = true; assertThrows(IOException.class, () -> load(image())); assertEquals(0, reads.get());
    }

    @Test public void downloadUsesDedicatedSessionAndOnePost() throws IOException {
        try (Response response = access.download(url(media, "/uploads/image.jpg").toString(), 123)) { assertEquals("download-body", response.body().string()); }
        assertEquals("download", lastGrant.get().get("purpose").getAsString());
        assertEquals(123, lastGrant.get().get("file").getAsInt());
        assertEquals(1, downloads.get()); assertEquals(0, reads.get()); assertEquals(0, installs.get());
    }

    @Test public void quotaErrorDoesNotReplayDownloadPost() {
        quota = true;
        IOException error = assertThrows(IOException.class, () -> access.download(url(media, "/uploads/image.jpg").toString(), 0));
        assertTrue(error.getMessage().contains("额度")); assertEquals(1, downloads.get());
    }

    @Test public void guestCannotDownloadWhenLoginRequired() {
        token.set(""); assertThrows(IOException.class, () -> access.download(url(media, "/uploads/image.jpg").toString(), 0));
        assertEquals(0, grants.get()); assertEquals(0, downloads.get());
    }

    @Test public void supportsV2SessionConfiguration() throws IOException {
        protocol = 2; load(image());
        assertEquals("/uploads/image.jpg", lastRead.get().getPath());
        assertEquals("b".repeat(64), lastGrant.get().get("sid").getAsString());
    }

    @Test public void externalImagesReceiveNoMediaCredentials() throws IOException, InterruptedException {
        try (MockWebServer external = new MockWebServer()) {
            external.start(java.net.InetAddress.getByName("127.0.0.1"), 0);
            external.enqueue(new MockResponse().setBody("external-image"));
            try (Response response = access.client().newCall(new Request.Builder().url(url(external, "/avatar.jpg")).build()).execute()) {
                assertEquals("external-image", response.body().string());
            }
            RecordedRequest read = external.takeRequest();
            assertNull(read.getHeader("Cookie")); assertNull(read.getHeader("Authorization")); assertNull(read.getHeader("Origin"));
            assertEquals(0, grants.get()); assertEquals(0, api.getRequestCount());
        }
    }

    @Test public void redirectToAnotherOriginDoesNotLeakPreviewCookie() throws IOException, InterruptedException {
        load(image());
        try (MockWebServer external = new MockWebServer()) {
            external.start(java.net.InetAddress.getByName("127.0.0.1"), 0);
            external.enqueue(new MockResponse().setBody("redirected"));
            media.setDispatcher(new Dispatcher() {
                @Override public MockResponse dispatch(RecordedRequest request) {
                    return new MockResponse().setResponseCode(302).addHeader("Location", url(external, "/image.jpg"));
                }
            });
            try (Response response = access.client().newCall(image()).execute()) { assertEquals("redirected", response.body().string()); }
            RecordedRequest read = external.takeRequest();
            assertNull(read.getHeader("Cookie")); assertNull(read.getHeader("Origin")); assertNull(read.getHeader("Referer"));
            assertNull(read.getHeader("Sec-Fetch-Site")); assertEquals(1, grants.get());
        }
    }
}
