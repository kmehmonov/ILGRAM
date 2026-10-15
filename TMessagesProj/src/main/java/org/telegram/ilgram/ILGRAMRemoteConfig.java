/*
 * ILGRAM — restricted-UI Telegram client for Istiqbol LUCK / Istiqbol LIGHT schools.
 * Based on Telegram for Android (https://github.com/DrKLO/Telegram), GNU GPL v2 or later.
 *
 * Verification and decryption of the remote allow-list published by the school's admin bot.
 * Pure Java (no Android classes) so it is unit-tested on a plain JVM.
 *
 * Envelope (JSON): {"format":"ilgram-config","v":1,"nonce":b64,"ct":b64,"sig":b64}
 *   sig = ECDSA-P256-SHA256 over AAD || nonce || ct   (DER), checked with the public key built into the APK
 *   ct  = AES-256-GCM(plaintext JSON, nonce, AAD), 128-bit tag
 * The signature is checked BEFORE decryption; anything that fails is rejected and the app keeps
 * using what it already has. Only the allowed group / user lists and the check interval can be
 * changed remotely — every other restriction is fixed in the APK.
 */
package org.telegram.ilgram;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ILGRAMRemoteConfig {

    public static final byte[] AAD = "ILGRAM-CONFIG-v1".getBytes(StandardCharsets.US_ASCII);
    public static final int MIN_INTERVAL_MINUTES = 15;
    public static final int MAX_INTERVAL_MINUTES = 7 * 24 * 60;
    public static final int DEFAULT_INTERVAL_MINUTES = 360;
    public static final int MAX_ENVELOPE_BYTES = 1024 * 1024;

    public final long version;
    public final String publishedAt;
    public final int checkIntervalMinutes;
    public final List<String> groups;
    public final List<String> users;

    private ILGRAMRemoteConfig(long version, String publishedAt, int interval, List<String> groups, List<String> users) {
        this.version = version;
        this.publishedAt = publishedAt;
        this.checkIntervalMinutes = interval;
        this.groups = groups;
        this.users = users;
    }

    /** The keys this config controls, in the same format as assets/ilgram.properties. */
    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty(ILGRAMRules.KEY_ALLOWED_GROUPS, join(groups));
        p.setProperty(ILGRAMRules.KEY_ALLOWED_USERS, join(users));
        return p;
    }

    @Override
    public String toString() {
        return "ILGRAMRemoteConfig{v" + version + ", groups=" + groups.size() + ", users=" + users.size() + ", interval=" + checkIntervalMinutes + "m}";
    }

    /** Thrown for every rejected envelope; the message says why (for logs only). */
    public static final class InvalidConfigException extends Exception {
        public InvalidConfigException(String message) {
            super(message);
        }

        public InvalidConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final String B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** Standard base64 (RFC 4648, padding optional). java.util.Base64 is not available below API 26. */
    public static byte[] b64(String s) throws InvalidConfigException {
        if (s == null) {
            throw new InvalidConfigException("bad base64");
        }
        String t = s.trim();
        while (t.endsWith("=")) {
            t = t.substring(0, t.length() - 1);
        }
        if (t.length() % 4 == 1) {
            throw new InvalidConfigException("bad base64");
        }
        byte[] out = new byte[t.length() * 3 / 4];
        int buf = 0, bits = 0, o = 0;
        for (int k = 0; k < t.length(); k++) {
            int v = B64.indexOf(t.charAt(k));
            if (v < 0) {
                throw new InvalidConfigException("bad base64");
            }
            buf = (buf << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[o++] = (byte) (buf >> bits);
                buf &= (1 << bits) - 1;
            }
        }
        return out;
    }

    /** Verifies, decrypts and validates an envelope. Never returns a partially trusted result. */
    public static ILGRAMRemoteConfig open(String envelope, byte[] publicKeyDer, byte[] aesKey) throws InvalidConfigException {
        if (envelope == null || envelope.length() > MAX_ENVELOPE_BYTES) {
            throw new InvalidConfigException("missing or oversized envelope");
        }
        if (publicKeyDer == null || aesKey == null || aesKey.length != 32) {
            throw new InvalidConfigException("keys not configured");
        }
        Map<String, Object> env = asObject(Json.parse(envelope), "envelope");
        if (!"ilgram-config".equals(env.get("format")) || !Long.valueOf(1).equals(env.get("v"))) {
            throw new InvalidConfigException("unknown envelope format");
        }
        byte[] nonce = b64(asString(env.get("nonce"), "nonce"));
        byte[] ct = b64(asString(env.get("ct"), "ct"));
        byte[] sig = b64(asString(env.get("sig"), "sig"));
        if (nonce.length != 12 || ct.length < 16) {
            throw new InvalidConfigException("bad nonce/ciphertext length");
        }
        byte[] signed = new byte[AAD.length + nonce.length + ct.length];
        System.arraycopy(AAD, 0, signed, 0, AAD.length);
        System.arraycopy(nonce, 0, signed, AAD.length, nonce.length);
        System.arraycopy(ct, 0, signed, AAD.length + nonce.length, ct.length);
        try {
            PublicKey pub = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(publicKeyDer));
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(pub);
            verifier.update(signed);
            if (!verifier.verify(sig)) {
                throw new InvalidConfigException("signature mismatch");
            }
        } catch (InvalidConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvalidConfigException("signature check failed", e);
        }
        String plaintext;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(AAD);
            plaintext = new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Throwable e) {
            throw new InvalidConfigException("decryption failed", e);
        }
        return fromPayload(plaintext);
    }

    static ILGRAMRemoteConfig fromPayload(String json) throws InvalidConfigException {
        Map<String, Object> p = asObject(Json.parse(json), "payload");
        if (!Long.valueOf(1).equals(p.get("schema"))) {
            throw new InvalidConfigException("unsupported schema");
        }
        long version = asLong(p.get("version"), "version");
        if (version <= 0) {
            throw new InvalidConfigException("bad version");
        }
        long interval = asLong(p.get("check_interval_minutes"), "check_interval_minutes");
        interval = Math.max(MIN_INTERVAL_MINUTES, Math.min(MAX_INTERVAL_MINUTES, interval));
        Map<String, Object> def = asObject(p.get("default"), "default");
        List<String> groups = ids(def.get("groups"), true);
        List<String> users = ids(def.get("users"), false);
        Object published = p.get("published_at");
        return new ILGRAMRemoteConfig(version, published instanceof String ? (String) published : "", (int) interval, groups, users);
    }

    private static List<String> ids(Object o, boolean groups) throws InvalidConfigException {
        if (!(o instanceof List)) {
            throw new InvalidConfigException("id list missing");
        }
        List<String> out = new ArrayList<>();
        for (Object v : (List<?>) o) {
            String s = v instanceof String ? (String) v : v instanceof Long ? String.valueOf(v) : null;
            if (s == null) {
                throw new InvalidConfigException("bad id");
            }
            boolean ok = groups ? ILGRAMRules.normalizeGroupId(s) != null : !ILGRAMRules.parseUserIds(s).isEmpty();
            if (ok) {
                out.add(s);
            }
        }
        return out;
    }

    private static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object o, String what) throws InvalidConfigException {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        throw new InvalidConfigException(what + " is not an object");
    }

    private static String asString(Object o, String what) throws InvalidConfigException {
        if (o instanceof String) {
            return (String) o;
        }
        throw new InvalidConfigException(what + " missing");
    }

    private static long asLong(Object o, String what) throws InvalidConfigException {
        if (o instanceof Long) {
            return (Long) o;
        }
        throw new InvalidConfigException(what + " missing");
    }

    /** Minimal strict JSON reader: objects, arrays, strings, integers, true/false/null. */
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        static Object parse(String s) throws InvalidConfigException {
            Json j = new Json(s);
            try {
                j.ws();
                Object v = j.value(0);
                j.ws();
                if (j.i != s.length()) {
                    throw new InvalidConfigException("trailing data");
                }
                return v;
            } catch (InvalidConfigException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new InvalidConfigException("bad json", e);
            }
        }

        private void ws() {
            while (i < s.length() && " \t\r\n".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
        }

        private Object value(int depth) throws InvalidConfigException {
            if (depth > 16) {
                throw new InvalidConfigException("json too deep");
            }
            char c = s.charAt(i);
            if (c == '{') {
                i++;
                Map<String, Object> m = new LinkedHashMap<>();
                ws();
                if (s.charAt(i) == '}') {
                    i++;
                    return m;
                }
                while (true) {
                    ws();
                    String k = string();
                    ws();
                    expect(':');
                    ws();
                    m.put(k, value(depth + 1));
                    ws();
                    if (s.charAt(i) == ',') {
                        i++;
                        continue;
                    }
                    expect('}');
                    return m;
                }
            }
            if (c == '[') {
                i++;
                List<Object> l = new ArrayList<>();
                ws();
                if (s.charAt(i) == ']') {
                    i++;
                    return l;
                }
                while (true) {
                    ws();
                    l.add(value(depth + 1));
                    ws();
                    if (s.charAt(i) == ',') {
                        i++;
                        continue;
                    }
                    expect(']');
                    return l;
                }
            }
            if (c == '"') {
                return string();
            }
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            int start = i;
            if (c == '-') {
                i++;
            }
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
                i++;
            }
            if (i < s.length() && ".eE".indexOf(s.charAt(i)) >= 0) {
                // fractions are never used in the config; skip them as an unusable number
                while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) {
                    i++;
                }
                return Double.NaN;
            }
            if (start == i || (c == '-' && start + 1 == i)) {
                throw new InvalidConfigException("unexpected character");
            }
            return Long.parseLong(s.substring(start, i));
        }

        private void expect(char c) throws InvalidConfigException {
            if (s.charAt(i) != c) {
                throw new InvalidConfigException("expected " + c);
            }
            i++;
        }

        private String string() throws InvalidConfigException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default:
                            throw new InvalidConfigException("bad escape");
                    }
                } else {
                    sb.append(c);
                }
            }
        }
    }
}
