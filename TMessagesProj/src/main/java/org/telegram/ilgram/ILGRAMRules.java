/*
 * ILGRAM — restricted-UI Telegram client for Istiqbol LUCK / Istiqbol LIGHT schools.
 * Based on Telegram for Android (https://github.com/DrKLO/Telegram), GNU GPL v2 or later.
 *
 * This class is intentionally pure Java (no Android imports) so that it can be unit tested
 * on a plain JVM. It contains the configuration model and every allow/deny decision that does
 * not need live Telegram objects. The Android-facing glue lives in ILGRAMPolicy.
 *
 * IMPORTANT: everything decided here is a USER INTERFACE restriction inside this app only.
 * It does not change what the Telegram account is allowed to do on Telegram's servers.
 */
package org.telegram.ilgram;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class ILGRAMRules {

    /** Telegram "Service notifications" account (login codes, security alerts). */
    public static final long SERVICE_NOTIFICATIONS_USER_ID = 777000L;

    /** Bot API encodes supergroups/channels as -100xxxxxxxxxx, i.e. -(1e12 + channelId). */
    private static final long BOT_API_CHANNEL_SHIFT = 1_000_000_000_000L;

    // ---- configuration keys (shared by assets/ilgram.properties and MDM managed configuration) ----
    public static final String KEY_STUDENT_MODE = "student_mode_enabled";
    public static final String KEY_ALLOWED_GROUPS = "allowed_group_ids";
    public static final String KEY_ALLOWED_USERS = "allowed_user_ids";
    public static final String KEY_ALLOW_SERVICE_NOTIFICATIONS = "allow_service_notifications";
    public static final String KEY_ALLOW_SECRET_CHATS = "allow_secret_chats";
    public static final String KEY_BROWSER = "browser_enabled";
    public static final String KEY_GLOBAL_SEARCH = "global_search_enabled";
    public static final String KEY_MULTI_ACCOUNT = "multi_account_enabled";
    public static final String KEY_HELP_TEXT = "help_text";

    public enum LinkDecision {
        /** A Telegram link that resolves to a chat/message/profile. The navigation guard still checks the target. */
        ALLOW_INTERNAL,
        /** An ordinary web link and the browser is enabled by configuration. */
        ALLOW_EXTERNAL,
        /** Not allowed in student mode. */
        BLOCK
    }

    public static final class Config {
        public final boolean studentModeEnabled;
        /** Normalised to the app's internal dialog ids: basic group -chatId, supergroup/channel -channelId. */
        public final Set<Long> allowedGroupDialogIds;
        public final Set<Long> allowedUserIds;
        public final boolean allowServiceNotifications;
        public final boolean allowSecretChats;
        public final boolean browserEnabled;
        public final boolean globalSearchEnabled;
        public final boolean multiAccountEnabled;
        public final String helpText;

        public Config(boolean studentModeEnabled, Set<Long> allowedGroupDialogIds, Set<Long> allowedUserIds,
                      boolean allowServiceNotifications, boolean allowSecretChats, boolean browserEnabled,
                      boolean globalSearchEnabled, boolean multiAccountEnabled, String helpText) {
            this.studentModeEnabled = studentModeEnabled;
            this.allowedGroupDialogIds = Collections.unmodifiableSet(new HashSet<>(allowedGroupDialogIds));
            this.allowedUserIds = Collections.unmodifiableSet(new HashSet<>(allowedUserIds));
            this.allowServiceNotifications = allowServiceNotifications;
            this.allowSecretChats = allowSecretChats;
            this.browserEnabled = browserEnabled;
            this.globalSearchEnabled = globalSearchEnabled;
            this.multiAccountEnabled = multiAccountEnabled;
            this.helpText = helpText == null ? "" : helpText;
        }

        /** Secure default used when no configuration can be read: student mode ON and nothing allowed. */
        public static Config lockedDown() {
            return new Config(true, Collections.<Long>emptySet(), Collections.<Long>emptySet(),
                    true, false, false, false, false, "");
        }

        @Override
        public String toString() {
            return "Config{studentMode=" + studentModeEnabled + ", groups=" + allowedGroupDialogIds.size()
                    + ", users=" + allowedUserIds.size() + ", serviceNotifications=" + allowServiceNotifications
                    + ", secretChats=" + allowSecretChats + ", browser=" + browserEnabled
                    + ", globalSearch=" + globalSearchEnabled + ", multiAccount=" + multiAccountEnabled + "}";
        }
    }

    private ILGRAMRules() {
    }

    // =====================================================================================
    // Configuration parsing
    // =====================================================================================

    /**
     * Builds a config from properties. Keys missing from {@code overrides} fall back to {@code base}.
     * Either argument may be null. Any parse problem falls back to the locked-down default for that key,
     * never to "allow everything".
     */
    public static Config parse(Properties base, Properties overrides) {
        Properties p = new Properties();
        if (base != null) {
            p.putAll(base);
        }
        if (overrides != null) {
            for (String key : overrides.stringPropertyNames()) {
                String value = overrides.getProperty(key);
                if (value != null && value.trim().length() > 0) {
                    p.setProperty(key, value);
                }
            }
        }
        return new Config(
                bool(p, KEY_STUDENT_MODE, true),
                parseGroupIds(p.getProperty(KEY_ALLOWED_GROUPS, "")),
                parseUserIds(p.getProperty(KEY_ALLOWED_USERS, "")),
                bool(p, KEY_ALLOW_SERVICE_NOTIFICATIONS, true),
                bool(p, KEY_ALLOW_SECRET_CHATS, false),
                bool(p, KEY_BROWSER, false),
                bool(p, KEY_GLOBAL_SEARCH, false),
                bool(p, KEY_MULTI_ACCOUNT, false),
                p.getProperty(KEY_HELP_TEXT, "")
        );
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        v = v.trim().toLowerCase(Locale.US);
        if (v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("on")) {
            return true;
        }
        if (v.equals("false") || v.equals("0") || v.equals("no") || v.equals("off")) {
            return false;
        }
        return def;
    }

    private static String[] tokens(String list) {
        if (list == null) {
            return new String[0];
        }
        return list.split("[\\s,;]+");
    }

    /**
     * Accepts group ids in the formats administrators usually have:
     * <ul>
     *     <li>Bot API supergroup/channel id: {@code -1001234567890}</li>
     *     <li>Bot API basic group id: {@code -123456789}</li>
     *     <li>Explicit prefixes: {@code channel:1234567890}, {@code chat:123456789}</li>
     * </ul>
     * Everything is converted to the internal dialog id (always negative). Invalid tokens are ignored.
     */
    public static Set<Long> parseGroupIds(String list) {
        Set<Long> out = new HashSet<>();
        for (String raw : tokens(list)) {
            Long id = normalizeGroupId(raw);
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    public static Long normalizeGroupId(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim().toLowerCase(Locale.US);
        if (t.isEmpty()) {
            return null;
        }
        try {
            if (t.startsWith("channel:") || t.startsWith("supergroup:")) {
                long id = Long.parseLong(t.substring(t.indexOf(':') + 1).trim());
                return id > 0 ? -id : null;
            }
            if (t.startsWith("chat:") || t.startsWith("group:")) {
                long id = Long.parseLong(t.substring(t.indexOf(':') + 1).trim());
                return id > 0 ? -id : null;
            }
            long v = Long.parseLong(t);
            if (v <= -BOT_API_CHANNEL_SHIFT) {
                long channelId = -v - BOT_API_CHANNEL_SHIFT;
                return channelId > 0 ? -channelId : null;
            }
            if (v < 0) {
                return v;
            }
            return null; // positive numbers are ambiguous for groups — rejected on purpose
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Set<Long> parseUserIds(String list) {
        Set<Long> out = new HashSet<>();
        for (String raw : tokens(list)) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            try {
                long v = Long.parseLong(raw.trim());
                if (v > 0) {
                    out.add(v);
                }
            } catch (NumberFormatException ignore) {
            }
        }
        return out;
    }

    // =====================================================================================
    // Dialog decisions
    // =====================================================================================

    /**
     * @param dialogId           internal dialog id (user &gt; 0, chat/channel &lt; 0). Encrypted dialogs must be
     *                           mapped to the peer user id by the caller and passed with {@code secretChat=true}.
     * @param selfUserId         the logged-in user (Saved Messages) — always hidden in student mode
     * @param extraAllowedGroups dynamically allowed groups (e.g. supergroups an allowed basic group migrated to); may be null
     */
    public static boolean isDialogAllowed(Config c, long dialogId, long selfUserId, boolean secretChat, Set<Long> extraAllowedGroups) {
        if (c == null) {
            return false;
        }
        if (!c.studentModeEnabled) {
            return true;
        }
        if (dialogId == 0) {
            return false;
        }
        if (dialogId > 0) {
            if (dialogId == selfUserId) {
                return false;
            }
            if (secretChat && !c.allowSecretChats) {
                return false;
            }
            if (dialogId == SERVICE_NOTIFICATIONS_USER_ID) {
                return c.allowServiceNotifications && !secretChat;
            }
            return c.allowedUserIds.contains(dialogId);
        }
        return c.allowedGroupDialogIds.contains(dialogId) || (extraAllowedGroups != null && extraAllowedGroups.contains(dialogId));
    }

    /** Service notifications are visible (to read login codes / security alerts) but students cannot write there. */
    public static boolean isReadOnlyDialog(long dialogId) {
        return dialogId == SERVICE_NOTIFICATIONS_USER_ID;
    }

    // =====================================================================================
    // Link decisions
    // =====================================================================================

    private static final Set<String> TME_HOSTS = setOf("t.me", "www.t.me", "telegram.me", "www.telegram.me", "telegram.dog", "www.telegram.dog");

    /** First path segments of t.me links that never lead to an ordinary chat. */
    private static final Set<String> TME_BLOCKED_SEGMENTS = setOf(
            "joinchat", "addstickers", "addemoji", "addlist", "proxy", "socks", "setlanguage", "addtheme", "bg",
            "share", "msg", "login", "confirmphone", "invoice", "boost", "giftcode", "contact", "m", "nft", "iv",
            "s", "a", "k", "z", "$", "stars", "premium", "premium_offer", "premium_multigift", "call", "addlist",
            "stickers", "emoji", "auth", "passport", "web", "apps", "dl", "blog", "faq", "privacy", "tos");

    /** Query parameters that turn a chat link into a bot start / mini app / call / join action. */
    private static final Set<String> BLOCKED_QUERY_PARAMS = setOf(
            "start", "startgroup", "startchannel", "startapp", "startattach", "attach", "game", "appname",
            "voicechat", "videochat", "livestream", "boost", "admin", "text", "story", "choose", "mode", "ref");

    private static final Set<String> TG_ALLOWED_HOSTS = setOf("resolve", "privatepost", "openmessage", "user");

    private static final Set<String> ALLOWED_TG_PARAMS = setOf(
            "domain", "phone", "post", "thread", "comment", "single", "t", "topic", "channel",
            "user_id", "chat_id", "message_id", "id");

    private static final Set<String> ALLOWED_TME_PARAMS = setOf("thread", "comment", "single", "t", "topic");

    public static LinkDecision classifyLink(Config c, String url) {
        if (c != null && !c.studentModeEnabled) {
            return isTelegramLink(url) ? LinkDecision.ALLOW_INTERNAL : LinkDecision.ALLOW_EXTERNAL;
        }
        if (url == null) {
            return LinkDecision.BLOCK;
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return LinkDecision.BLOCK;
        }
        String lower = u.toLowerCase(Locale.US);

        if (lower.startsWith("tg:")) {
            return classifyTg(u) ? LinkDecision.ALLOW_INTERNAL : LinkDecision.BLOCK;
        }

        String noScheme;
        if (lower.startsWith("https://")) {
            noScheme = u.substring(8);
        } else if (lower.startsWith("http://")) {
            noScheme = u.substring(7);
        } else if (lower.indexOf("://") < 0 && !lower.contains(":") && lower.matches("^(www\\.)?(t\\.me|telegram\\.me|telegram\\.dog)/.*")) {
            noScheme = u; // "t.me/xxx" without a scheme, as it appears in message text
        } else {
            // mailto:, tel:, geo:, intent:, tonsite:, file:, content: ...
            return LinkDecision.BLOCK;
        }

        int slash = indexOfAny(noScheme, "/?#");
        String host = (slash < 0 ? noScheme : noScheme.substring(0, slash)).toLowerCase(Locale.US);
        int at = host.lastIndexOf('@');
        if (at >= 0) {
            host = host.substring(at + 1);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        String rest = slash < 0 ? "" : noScheme.substring(slash);

        if (TME_HOSTS.contains(host)) {
            return classifyTmePath(rest) ? LinkDecision.ALLOW_INTERNAL : LinkDecision.BLOCK;
        }
        if (host.endsWith(".t.me")) {
            // username.t.me — subdomain form of a public username link
            String username = host.substring(0, host.length() - ".t.me".length());
            if (isUsername(username) && (rest.isEmpty() || rest.equals("/"))) {
                return LinkDecision.ALLOW_INTERNAL;
            }
            return LinkDecision.BLOCK;
        }
        return c != null && c.browserEnabled ? LinkDecision.ALLOW_EXTERNAL : LinkDecision.BLOCK;
    }

    private static boolean isTelegramLink(String url) {
        if (url == null) {
            return false;
        }
        String l = url.trim().toLowerCase(Locale.US);
        return l.startsWith("tg:") || l.matches("^(https?://)?(www\\.)?(t\\.me|telegram\\.me|telegram\\.dog)(/.*)?$") || l.matches("^(https?://)?[a-z0-9_]+\\.t\\.me(/.*)?$");
    }

    private static boolean classifyTg(String url) {
        // tg:resolve?domain=x  or  tg://resolve?domain=x
        String s = url.substring(3);
        if (s.startsWith("//")) {
            s = s.substring(2);
        }
        int q = s.indexOf('?');
        String host = (q < 0 ? s : s.substring(0, q)).toLowerCase(Locale.US);
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        if (!TG_ALLOWED_HOSTS.contains(host)) {
            return false;
        }
        String query = q < 0 ? "" : s.substring(q + 1);
        for (String key : queryKeys(query)) {
            if (!ALLOWED_TG_PARAMS.contains(key)) {
                return false;
            }
        }
        return true;
    }

    private static boolean classifyTmePath(String rest) {
        String path = rest;
        String query = "";
        int hash = path.indexOf('#');
        if (hash >= 0) {
            path = path.substring(0, hash);
        }
        int q = path.indexOf('?');
        if (q >= 0) {
            query = path.substring(q + 1);
            path = path.substring(0, q);
        }
        for (String key : queryKeys(query)) {
            if (BLOCKED_QUERY_PARAMS.contains(key) || !ALLOWED_TME_PARAMS.contains(key)) {
                return false;
            }
        }
        String[] seg = splitPath(path);
        if (seg.length == 0) {
            return false;
        }
        String first = seg[0];
        String firstLower = first.toLowerCase(Locale.US);
        if (first.startsWith("+")) {
            // t.me/+998901234567 is a phone-number link to a user; t.me/+AbCdEf is a private invite link
            return seg.length == 1 && first.substring(1).matches("[0-9]{5,16}");
        }
        if (TME_BLOCKED_SEGMENTS.contains(firstLower)) {
            return false;
        }
        if (firstLower.equals("c")) {
            // t.me/c/<channelId>[/<postId>[/<postId>]] — private chat/post link
            if (seg.length < 2 || seg.length > 4) {
                return false;
            }
            for (int i = 1; i < seg.length; i++) {
                if (!seg[i].matches("[0-9]{1,19}")) {
                    return false;
                }
            }
            return true;
        }
        if (!isUsername(first)) {
            return false;
        }
        // t.me/<username>[/<postId>[/<postId>]]; anything non-numeric (e.g. /appname, /s/123 stories) is blocked
        if (seg.length > 3) {
            return false;
        }
        for (int i = 1; i < seg.length; i++) {
            if (!seg[i].matches("[0-9]{1,10}")) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUsername(String s) {
        return s != null && s.matches("[A-Za-z][A-Za-z0-9_]{2,31}");
    }

    private static String[] splitPath(String path) {
        String p = path;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.isEmpty()) {
            return new String[0];
        }
        return p.split("/");
    }

    private static Set<String> queryKeys(String query) {
        Set<String> keys = new HashSet<>();
        if (query == null || query.isEmpty()) {
            return keys;
        }
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            keys.add((eq < 0 ? part : part.substring(0, eq)).toLowerCase(Locale.US));
        }
        return keys;
    }

    private static int indexOfAny(String s, String chars) {
        int best = -1;
        for (int i = 0; i < chars.length(); i++) {
            int idx = s.indexOf(chars.charAt(i));
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }

    private static Set<String> setOf(String... values) {
        Set<String> s = new HashSet<>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }

    // =====================================================================================
    // Settings visibility
    // =====================================================================================

    /** Keys used by the simplified Settings screen. */
    public static final String SETTING_ACCOUNT = "account";
    public static final String SETTING_CHAT = "chat";
    public static final String SETTING_PRIVACY = "privacy";
    public static final String SETTING_NOTIFICATIONS = "notifications";
    public static final String SETTING_DATA = "data";
    public static final String SETTING_FOLDERS = "folders";
    public static final String SETTING_DEVICES = "devices";
    public static final String SETTING_POWER = "power";
    public static final String SETTING_LANGUAGE = "language";
    public static final String SETTING_PREMIUM = "premium";
    public static final String SETTING_STARS = "stars";
    public static final String SETTING_BUSINESS = "business";
    public static final String SETTING_GIFTS = "gifts";
    public static final String SETTING_WALLET = "wallet";
    public static final String SETTING_ACCOUNTS = "accounts";
    public static final String SETTING_ASK_QUESTION = "ask_question";
    public static final String SETTING_FAQ = "faq";
    public static final String SETTING_FEATURES = "features";
    public static final String SETTING_PRIVACY_POLICY = "privacy_policy";
    public static final String SETTING_PROXY = "proxy";
    public static final String SETTING_DEBUG = "debug";

    private static final Set<String> STUDENT_VISIBLE_SETTINGS = setOf(
            SETTING_ACCOUNT, SETTING_CHAT, SETTING_PRIVACY, SETTING_NOTIFICATIONS, SETTING_DATA,
            SETTING_DEVICES, SETTING_POWER, SETTING_LANGUAGE);

    public static boolean isSettingVisible(Config c, String key) {
        if (c == null || !c.studentModeEnabled) {
            return true;
        }
        if (SETTING_ACCOUNTS.equals(key)) {
            return c.multiAccountEnabled;
        }
        return STUDENT_VISIBLE_SETTINGS.contains(key);
    }
}
