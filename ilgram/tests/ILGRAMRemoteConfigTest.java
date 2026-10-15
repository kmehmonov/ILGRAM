import org.telegram.ilgram.ILGRAMRemoteConfig;
import org.telegram.ilgram.ILGRAMRules;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Cross-language test: envelopes produced by the admin bot's Python code are opened by the app's Java code. */
public class ILGRAMRemoteConfigTest {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; } else { failed++; System.out.println("FAIL: " + name); }
    }

    static String read(Path dir, String f) throws Exception {
        return new String(Files.readAllBytes(dir.resolve(f)), StandardCharsets.UTF_8).trim();
    }

    static boolean rejects(String env, byte[] pub, byte[] aes) {
        try {
            ILGRAMRemoteConfig.open(env, pub, aes);
            return false;
        } catch (ILGRAMRemoteConfig.InvalidConfigException e) {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]);
        byte[] pub = ILGRAMRemoteConfig.b64(read(dir, "public_key.b64"));
        byte[] aes = ILGRAMRemoteConfig.b64(read(dir, "aes_key.b64"));

        ILGRAMRemoteConfig c = ILGRAMRemoteConfig.open(read(dir, "valid_v3.json"), pub, aes);
        check("version", c.version == 3);
        check("interval", c.checkIntervalMinutes == 60);
        check("groups", c.groups.size() == 2 && c.groups.contains("-1001234567890"));
        check("users", c.users.size() == 2 && c.users.contains("7012345678"));
        Properties p = c.toProperties();
        ILGRAMRules.Config rc = ILGRAMRules.parse(p, null);
        check("rules see supergroup", rc.allowedGroupDialogIds.contains(-1234567890L));
        check("rules see basic group", rc.allowedGroupDialogIds.contains(-987654321L));
        check("rules see users", rc.allowedUserIds.contains(111111111L) && rc.allowedUserIds.contains(7012345678L));

        ILGRAMRemoteConfig c2 = ILGRAMRemoteConfig.open(read(dir, "valid_v2.json"), pub, aes);
        check("interval clamped to minimum", c2.checkIntervalMinutes == ILGRAMRemoteConfig.MIN_INTERVAL_MINUTES);
        check("empty group list stays empty", c2.groups.isEmpty() && c2.toProperties().getProperty("allowed_group_ids").isEmpty());
        check("unicode payload", ILGRAMRemoteConfig.open(read(dir, "unicode.json"), pub, aes).publishedAt.equals("O'zbek ✓ \"q\""));

        check("tampered ciphertext rejected", rejects(read(dir, "tampered.json"), pub, aes));
        check("foreign signer rejected", rejects(read(dir, "wrong_signer.json"), pub, aes));
        byte[] badAes = aes.clone(); badAes[0] ^= 1;
        check("wrong aes key rejected", rejects(read(dir, "valid_v3.json"), pub, badAes));
        check("missing keys rejected", rejects(read(dir, "valid_v3.json"), null, aes));
        check("garbage rejected", rejects("{\"format\":\"x\"}", pub, aes));
        check("not json rejected", rejects("<html>", pub, aes));
        check("empty rejected", rejects("", pub, aes));
        check("null rejected", rejects(null, pub, aes));

        byte[] hello = ILGRAMRemoteConfig.b64("aGVsbG8=");
        check("base64", new String(hello, StandardCharsets.US_ASCII).equals("hello"));
        check("base64 no padding", new String(ILGRAMRemoteConfig.b64("aGk"), StandardCharsets.US_ASCII).equals("hi"));

        System.out.println("ILGRAMRemoteConfigTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
