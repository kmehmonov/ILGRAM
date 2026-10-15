/*
 * ILGRAM — restricted-UI Telegram client for Istiqbol LUCK / Istiqbol LIGHT schools.
 * Based on Telegram for Android (https://github.com/DrKLO/Telegram), GNU GPL v2 or later.
 *
 * Periodically downloads the school's signed allow-list (see ILGRAMRemoteConfig) and hands it to
 * ILGRAMPolicy. Rules:
 *  - only an envelope signed by the key built into this APK is accepted;
 *  - only a NEWER version than the one already applied is accepted (no replay / downgrade);
 *  - on any error (offline, server down, bad data) the app keeps its current lists;
 *  - checks on start, when the app comes to the foreground, and every N minutes (N comes from the
 *    published config, 15 minutes .. 7 days). The keep-alive service keeps the process running.
 * The request carries no user data: it is a plain GET of one file.
 */
package org.telegram.ilgram;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ILGRAMRemoteSync {

    public static final String KEY_URL = "remote_config_url";
    public static final String KEY_PUBLIC_KEY = "remote_config_public_key";
    public static final String KEY_AES_KEY = "remote_config_aes_key";

    private static final String CACHE_FILE = "ilgram_remote_config.json";
    private static final String PREFS = "ilgram_remote";
    private static final long FOREGROUND_MIN_GAP_MS = 2 * 60 * 1000L;
    private static final long RETRY_AFTER_ERROR_MIN = ILGRAMRemoteConfig.MIN_INTERVAL_MINUTES;

    private static volatile ILGRAMRemoteSync instance;

    private final Context context;
    private final String url;
    private final byte[] publicKey;
    private final byte[] aesKey;
    private final ScheduledExecutorService executor;
    private volatile ILGRAMRemoteConfig current;
    private volatile long lastAttempt;
    private ScheduledFuture<?> next;

    private ILGRAMRemoteSync(Context context, String url, byte[] publicKey, byte[] aesKey) {
        this.context = context;
        this.url = url;
        this.publicKey = publicKey;
        this.aesKey = aesKey;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ilgram-remote-config");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    /**
     * Called by ILGRAMPolicy once the bundled properties are loaded. Returns null (feature off) when
     * the APK was built without remote-config keys.
     */
    public static synchronized ILGRAMRemoteSync create(Context app, Properties bundled) {
        if (instance != null) {
            return instance;
        }
        String url = bundled.getProperty(KEY_URL, "").trim();
        String pub = bundled.getProperty(KEY_PUBLIC_KEY, "").trim();
        String aes = bundled.getProperty(KEY_AES_KEY, "").trim();
        if (url.isEmpty() || pub.isEmpty() || aes.isEmpty() || !url.startsWith("https://")) {
            FileLog.d("ILGRAM remote config: not configured in this build");
            return null;
        }
        try {
            instance = new ILGRAMRemoteSync(app, url, ILGRAMRemoteConfig.b64(pub), ILGRAMRemoteConfig.b64(aes));
            instance.loadCache();
            return instance;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    public static ILGRAMRemoteSync get() {
        return instance;
    }

    /** The currently applied remote config, or null when none has been received yet. */
    public ILGRAMRemoteConfig current() {
        return current;
    }

    /** Remote keys to merge over the bundled config, or null. */
    public Properties properties() {
        ILGRAMRemoteConfig c = current;
        return c != null ? c.toProperties() : null;
    }

    // ------------------------------------------------------------------------------------------ cache

    private File cacheFile() {
        return new File(context.getFilesDir(), CACHE_FILE);
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void loadCache() {
        File f = cacheFile();
        if (!f.exists()) {
            return;
        }
        try {
            String env = new String(readAll(new java.io.FileInputStream(f), ILGRAMRemoteConfig.MAX_ENVELOPE_BYTES), StandardCharsets.UTF_8);
            current = ILGRAMRemoteConfig.open(env, publicKey, aesKey); // re-verified: a modified cache is ignored
            FileLog.d("ILGRAM remote config from cache: " + current);
        } catch (Throwable e) {
            FileLog.e("ILGRAM remote config cache rejected", e);
            current = null;
            prefs().edit().remove("etag").apply();
        }
    }

    // ------------------------------------------------------------------------------------------ schedule

    /** Starts the periodic check. The first check runs shortly after start-up. */
    synchronized void start() {
        schedule(10);
    }

    /** App came to the foreground: check now unless we checked a moment ago. */
    public static void onAppForeground() {
        ILGRAMRemoteSync s = instance;
        if (s != null && SystemClock.elapsedRealtime() - s.lastAttempt > FOREGROUND_MIN_GAP_MS) {
            s.schedule(0);
        }
    }

    private synchronized void schedule(long delaySeconds) {
        if (next != null) {
            next.cancel(false);
        }
        next = executor.schedule(this::runCheck, delaySeconds, TimeUnit.SECONDS);
    }

    private void runCheck() {
        boolean ok = false;
        try {
            ok = checkNow();
        } catch (Throwable e) {
            FileLog.e(e);
        }
        ILGRAMRemoteConfig c = current;
        long minutes = c != null ? c.checkIntervalMinutes : ILGRAMRemoteConfig.DEFAULT_INTERVAL_MINUTES;
        if (!ok) {
            minutes = Math.min(minutes, RETRY_AFTER_ERROR_MIN);
        }
        schedule(minutes * 60);
    }

    // ------------------------------------------------------------------------------------------ fetch

    /** One download attempt. Returns false only for errors worth retrying soon (network/server). */
    public boolean checkNow() {
        lastAttempt = SystemClock.elapsedRealtime();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept", "application/json");
            String etag = prefs().getString("etag", null);
            if (etag != null && current != null) {
                conn.setRequestProperty("If-None-Match", etag);
            }
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return true;
            }
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return true; // nothing published yet: keep the bundled lists
            }
            if (code != HttpURLConnection.HTTP_OK) {
                FileLog.d("ILGRAM remote config: HTTP " + code);
                return false;
            }
            byte[] body = readAll(conn.getInputStream(), ILGRAMRemoteConfig.MAX_ENVELOPE_BYTES);
            String env = new String(body, StandardCharsets.UTF_8);
            return accept(env, conn.getHeaderField("ETag"));
        } catch (Throwable e) {
            FileLog.d("ILGRAM remote config: " + e);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** Verifies and, if newer, stores and applies an envelope. Package-private for tests. */
    public boolean accept(String envelope, String etag) {
        ILGRAMRemoteConfig cfg;
        try {
            cfg = ILGRAMRemoteConfig.open(envelope, publicKey, aesKey);
        } catch (ILGRAMRemoteConfig.InvalidConfigException e) {
            FileLog.e("ILGRAM remote config rejected: " + e.getMessage());
            return true; // not a network problem; a retry would get the same bytes
        }
        ILGRAMRemoteConfig old = current;
        if (old != null && cfg.version <= old.version) {
            if (cfg.version == old.version && etag != null) {
                prefs().edit().putString("etag", etag).apply();
            }
            return true;
        }
        try {
            File tmp = new File(context.getFilesDir(), CACHE_FILE + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(envelope.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if (!tmp.renameTo(cacheFile())) {
                FileLog.e("ILGRAM remote config: cache rename failed");
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        SharedPreferences.Editor ed = prefs().edit();
        if (etag != null) {
            ed.putString("etag", etag);
        } else {
            ed.remove("etag");
        }
        ed.apply();
        current = cfg;
        FileLog.d("ILGRAM remote config applied: " + cfg);
        ILGRAMPolicy.onRemoteConfigChanged();
        return true;
    }

    private static byte[] readAll(InputStream in, int limit) throws java.io.IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > limit) {
                    throw new java.io.IOException("response too large");
                }
            }
            return out.toByteArray();
        }
    }

    /** Instrumentation tests only. */
    public static synchronized ILGRAMRemoteSync createForTesting(Context app, String url, byte[] pub, byte[] aes) {
        ILGRAMRemoteSync s = new ILGRAMRemoteSync(app, url, pub, aes);
        s.cacheFile().delete();
        s.prefs().edit().clear().commit();
        instance = s;
        return s;
    }

    /** Instrumentation tests only: forgets the instance but keeps its cache (simulates a process restart). */
    public static synchronized void dropInstanceForTesting() {
        if (instance != null) {
            instance.executor.shutdownNow();
        }
        instance = null;
    }

    /** Instrumentation tests only. */
    public static synchronized void resetForTesting() {
        if (instance != null) {
            instance.executor.shutdownNow();
            instance.cacheFile().delete();
            instance.prefs().edit().clear().commit();
        }
        instance = null;
    }
}
