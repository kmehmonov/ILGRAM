import org.telegram.ilgram.ILGRAMRules;
import org.telegram.ilgram.ILGRAMRules.Config;
import org.telegram.ilgram.ILGRAMRules.LinkDecision;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Plain-JVM tests for ILGRAMRules (no JUnit needed). Run: see ilgram/tests/run-tests.sh */
public class ILGRAMRulesTest {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; } else { failed++; System.out.println("FAIL: " + name); }
    }

    static Config cfg(String groups, String users) {
        Properties p = new Properties();
        p.setProperty("student_mode_enabled", "true");
        p.setProperty("allowed_group_ids", groups);
        p.setProperty("allowed_user_ids", users);
        return ILGRAMRules.parse(p, null);
    }

    public static void main(String[] args) {
        final long SELF = 5555L;
        Config c = cfg("-1001234567890, -987654321 channel:42", "111 222,333");

        // --- id parsing ---
        check("supergroup bot-api id", c.allowedGroupDialogIds.contains(-1234567890L));
        check("basic group id", c.allowedGroupDialogIds.contains(-987654321L));
        check("channel: prefix", c.allowedGroupDialogIds.contains(-42L));
        check("group count", c.allowedGroupDialogIds.size() == 3);
        check("users parsed", c.allowedUserIds.size() == 3 && c.allowedUserIds.contains(222L));
        check("garbage ignored", ILGRAMRules.parseGroupIds("abc, 12345, , -").isEmpty());
        check("positive group id rejected", ILGRAMRules.normalizeGroupId("1001234567890") == null);
        check("negative user ignored", ILGRAMRules.parseUserIds("-5 7").equals(Collections.singleton(7L)));

        // --- dialogs (tests 2-6 of the spec, at rule level) ---
        check("allowed group visible", ILGRAMRules.isDialogAllowed(c, -1234567890L, SELF, false, null));
        check("foreign group hidden", !ILGRAMRules.isDialogAllowed(c, -1999999999L, SELF, false, null));
        check("allowed user visible", ILGRAMRules.isDialogAllowed(c, 111L, SELF, false, null));
        check("foreign user hidden", !ILGRAMRules.isDialogAllowed(c, 444L, SELF, false, null));
        check("saved messages hidden", !ILGRAMRules.isDialogAllowed(c, SELF, SELF, false, null));
        check("service notifications visible by default", ILGRAMRules.isDialogAllowed(c, 777000L, SELF, false, null));
        check("secret chat hidden by default", !ILGRAMRules.isDialogAllowed(c, 111L, SELF, true, null));
        check("zero dialog hidden", !ILGRAMRules.isDialogAllowed(c, 0, SELF, false, null));
        Set<Long> migrated = new HashSet<>(); migrated.add(-777L);
        check("migrated supergroup allowed via extra set", ILGRAMRules.isDialogAllowed(c, -777L, SELF, false, migrated));
        check("name is irrelevant: only id decides", !ILGRAMRules.isDialogAllowed(c, -1L, SELF, false, null));

        Properties off = new Properties(); off.setProperty("student_mode_enabled", "false");
        Config open = ILGRAMRules.parse(null, off);
        check("student mode off allows everything", ILGRAMRules.isDialogAllowed(open, 444L, SELF, true, null));

        // overrides (MDM) win over base; empty override does not erase base
        Properties base = new Properties(); base.setProperty("allowed_user_ids", "1"); base.setProperty("allowed_group_ids", "-5");
        Properties over = new Properties(); over.setProperty("allowed_user_ids", "2"); over.setProperty("allowed_group_ids", "  ");
        Config merged = ILGRAMRules.parse(base, over);
        check("override replaces users", merged.allowedUserIds.equals(Collections.singleton(2L)));
        check("empty override keeps base groups", merged.allowedGroupDialogIds.equals(Collections.singleton(-5L)));
        check("unparseable bool -> secure default", ILGRAMRules.parse(props("student_mode_enabled", "maybe"), null).studentModeEnabled);
        check("locked down default", ILGRAMRules.Config.lockedDown().allowedUserIds.isEmpty() && ILGRAMRules.Config.lockedDown().studentModeEnabled);

        // --- links (tests 11-15, 29 at rule level) ---
        LinkDecision A = LinkDecision.ALLOW_INTERNAL, B = LinkDecision.BLOCK;
        String[][] cases = {
            {"https://t.me/istiqbol_luck_9a", "A"},
            {"t.me/istiqbol_luck_9a/15", "A"},
            {"https://t.me/c/1234567890/77", "A"},
            {"https://t.me/c/1234567890/77/80?thread=3", "A"},
            {"tg://resolve?domain=istiqbol", "A"},
            {"tg://privatepost?channel=123&post=5", "A"},
            {"tg://user?id=111", "A"},
            {"https://t.me/+998901234567", "A"},
            {"https://t.me/+AbCdEfGhIj", "B"},
            {"https://t.me/joinchat/AbCdEf", "B"},
            {"tg://join?invite=AbCdEf", "B"},
            {"https://t.me/somebot?start=abc", "B"},
            {"https://t.me/somebot?startapp=x", "B"},
            {"https://t.me/somebot/myapp", "B"},
            {"tg://resolve?domain=somebot&startapp=1", "B"},
            {"tg://resolve?domain=somebot&start=1", "B"},
            {"https://t.me/proxy?server=1.2.3.4&port=443&secret=00", "B"},
            {"tg://proxy?server=1.2.3.4", "B"},
            {"tg://socks?server=1.2.3.4", "B"},
            {"https://t.me/addlist/abc", "B"},
            {"https://t.me/addstickers/abc", "B"},
            {"https://t.me/durov/s/12", "B"},
            {"https://t.me/share/url?url=x", "B"},
            {"tg://msg_url?url=x", "B"},
            {"tg://login?token=abc", "B"},
            {"https://t.me/login/abc", "B"},
            {"https://google.com", "B"},
            {"http://example.org/page", "B"},
            {"https://telegra.ph/article", "B"},
            {"mailto:a@b.c", "B"},
            {"tel:+998901234567", "B"},
            {"intent://scan/#Intent;scheme=zxing;end", "B"},
            {"tonsite://abc.ton", "B"},
            {"https://evil.com/?u=https://t.me/x", "B"},
            {"https://t.me.evil.com/istiqbol", "B"},
            {"https://user@t.me.evil.com/x", "B"},
            {"https://istiqbol.t.me", "A"},
            {"https://istiqbol.t.me/abc", "B"},
            {"", "B"},
            {"https://t.me/", "B"},
            {"https://t.me/iv?url=x", "B"},
            {"https://t.me/boost/abc", "B"},
            {"https://t.me/istiqbol?voicechat", "B"},
            {"https://t.me/istiqbol?livestream", "B"},
            {"https://t.me/m/abcdef", "B"},
        };
        for (String[] tc : cases) {
            LinkDecision expected = tc[1].equals("A") ? A : B;
            LinkDecision got = ILGRAMRules.classifyLink(c, tc[0]);
            check("link " + tc[0] + " expected " + expected + " got " + got, got == expected);
        }
        Config browser = ILGRAMRules.parse(props("browser_enabled", "true"), null);
        check("browser enabled allows external", ILGRAMRules.classifyLink(browser, "https://google.com") == LinkDecision.ALLOW_EXTERNAL);
        check("browser enabled still blocks invite", ILGRAMRules.classifyLink(browser, "https://t.me/+AbCdEf") == LinkDecision.BLOCK);

        // --- settings ---
        check("language visible", ILGRAMRules.isSettingVisible(c, ILGRAMRules.SETTING_LANGUAGE));
        check("folders hidden", !ILGRAMRules.isSettingVisible(c, ILGRAMRules.SETTING_FOLDERS));
        check("premium hidden", !ILGRAMRules.isSettingVisible(c, ILGRAMRules.SETTING_PREMIUM));
        check("proxy hidden", !ILGRAMRules.isSettingVisible(c, ILGRAMRules.SETTING_PROXY));
        check("accounts hidden", !ILGRAMRules.isSettingVisible(c, ILGRAMRules.SETTING_ACCOUNTS));
        check("devices visible", ILGRAMRules.isSettingVisible(c, ILGRAMRules.SETTING_DEVICES));

        System.out.println("ILGRAMRulesTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static Properties props(String k, String v) { Properties p = new Properties(); p.setProperty(k, v); return p; }
}
