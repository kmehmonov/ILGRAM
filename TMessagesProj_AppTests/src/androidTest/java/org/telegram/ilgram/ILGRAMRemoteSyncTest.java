/*
 * ILGRAM remote allow-list on a real Android runtime: crypto provider, cache, version rules, policy merge.
 * Envelopes are the cross-language fixtures from ilgram/tests/fixtures/remote (TEST keys, made by the
 * admin bot's code; regenerate with ilgram/tests/make_remote_fixture.py and update the constants).
 */
package org.telegram.ilgram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

@RunWith(AndroidJUnit4.class)
public class ILGRAMRemoteSyncTest {

    static final String PUB = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9Jx5vIzGp27AoQpU9W38kuVMnzdnFwbhGlpKpahJbalndL+GM1ULtnOCL9M989FZIzSPlOSWGuOhij1eK+nLIw==";
    static final String AES = "vzrYFzczjOpsYOIDZB7Jvs81DA6EefPJrqATDYP+718=";
    static final String V3 = "{\"format\":\"ilgram-config\",\"v\":1,\"nonce\":\"bX8imhumN3UyjM+S\",\"ct\":\"AdhMr6lgr6Nqer5GouLqdeH1bsxQjFYTakALLUaKLJX88lTAyXiACdDvokyaWjsk8eyMpNccpdq8evRhUWT3qtimQwAgEKd1Arr/MLPzItluohSRbhNhY1/ykm1ZuVw4HR9LgYFFM6guyRPHGLGPobGnkvW27iN+Oi6hfu+5GCSC8KMIDK+SR3s5fDCKI4LGbiiB33x20OAW5+5m2x1sNRbIhkC0zuN2DfplpIwlwB/v7+t4IM3uZkSNWb+sIwluhRDkm4ap4zRWPl7lfk8MhQ==\",\"sig\":\"MEUCIQCG+eCr49t6x84Tx8myiADYinNQw9LCv1mgQ0jXAdsuMQIgAYzEc6xEH9uS+j7A2263WRq6ZzBvxe7JDLHkZgJhxn4=\"}";
    static final String V2 = "{\"format\":\"ilgram-config\",\"v\":1,\"nonce\":\"igOI3GEV5dvx68Mu\",\"ct\":\"wLFTaseV5NeuVERt6TRd2+eFTkIlLww32MadUOSUcWzXnFkQ8qJ4Z+Jj0jdq9QrdhZf/lsAc0sU56aXKrIflISCOS8bYUglXxjnt8dj47WI4B+qetPgN6+uJ5vWkyrnL34Ws+Tjrk1XmZpw1/owDNTMOKpV/xqNSPw0trMZKLYu9ueRiNghRwr0i1R0ZS7jYcPUBaXpRoGqfzlgyug==\",\"sig\":\"MEUCIAXcKLvxiqfuwKE8/pPEWysIRfHHuHEcXb6tAYUYS62PAiEAvDuqX53CLrt3YvG7OoAdb3bZl9LcplNgi6GjTs/YEN0=\"}";
    static final String TAMPERED = "{\"format\": \"ilgram-config\", \"v\": 1, \"nonce\": \"bX8imhumN3UyjM+S\", \"ct\": \"ANhMr6lgr6Nqer5GouLqdeH1bsxQjFYTakALLUaKLJX88lTAyXiACdDvokyaWjsk8eyMpNccpdq8evRhUWT3qtimQwAgEKd1Arr/MLPzItluohSRbhNhY1/ykm1ZuVw4HR9LgYFFM6guyRPHGLGPobGnkvW27iN+Oi6hfu+5GCSC8KMIDK+SR3s5fDCKI4LGbiiB33x20OAW5+5m2x1sNRbIhkC0zuN2DfplpIwlwB/v7+t4IM3uZkSNWb+sIwluhRDkm4ap4zRWPl7lfk8MhQ==\", \"sig\": \"MEUCIQCG+eCr49t6x84Tx8myiADYinNQw9LCv1mgQ0jXAdsuMQIgAYzEc6xEH9uS+j7A2263WRq6ZzBvxe7JDLHkZgJhxn4=\"}";
    static final String WRONG_SIGNER = "{\"format\":\"ilgram-config\",\"v\":1,\"nonce\":\"2WTV+mFcSIIpxcB1\",\"ct\":\"wl17Bh0MtE9tUu1f/ZFz/LP2ZN5Zx9LngxE4PEkhZ8QnD1swPF4c3HC9QhFYIiMzKn/4ezOv159dK+sqx/CD4K62TKpsz5BugiQhuEWukZk/6VldYJvR7z9JO7ynVegdomhuPgcRkItrlVizQKtrZXMh7TXNd91HE/3+Fdeg/VE1tZ1tJxwI5f7hd/C0NYDQBdoDqjjK5Z/rx9iFJvcHpPJuHVKlu6nBPtLYUlxSfuwTPYkGdA2rqzVX8C4dSZ6UTwYut8n7kY3s5myXqFEYg50=\",\"sig\":\"MEMCIHDKbbFrrgmGGF1lBUnpqzdf8Z0yQkW6g7H9wyGkSAWoAh9MS4ngKKoPq0fChSw2avIOsZKAWs/XgJBwcW+vwb07\"}";

    private Context app;

    @Before
    public void setUp() {
        app = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        ILGRAMRemoteSync.resetForTesting();
    }

    @After
    public void tearDown() {
        ILGRAMRemoteSync.resetForTesting();
        ILGRAMPolicy.setConfigForTesting(null);
    }

    private ILGRAMRemoteSync sync() throws Exception {
        return ILGRAMRemoteSync.createForTesting(app, "https://127.0.0.1:1/config",
                ILGRAMRemoteConfig.b64(PUB), ILGRAMRemoteConfig.b64(AES));
    }

    @Test
    public void androidCryptoOpensBotEnvelope() throws Exception {
        ILGRAMRemoteConfig c = ILGRAMRemoteConfig.open(V3, ILGRAMRemoteConfig.b64(PUB), ILGRAMRemoteConfig.b64(AES));
        assertEquals(3, c.version);
        assertEquals(60, c.checkIntervalMinutes);
        assertTrue(c.groups.contains("-1001234567890"));
    }

    @Test
    public void acceptAppliesToPolicy() throws Exception {
        ILGRAMRemoteSync s = sync();
        assertTrue(s.accept(V3, "\"e3\""));
        assertEquals(3, s.current().version);
        ILGRAMPolicy.reloadForTesting(app);
        ILGRAMRules.Config c = ILGRAMPolicy.config();
        assertTrue(c.allowedGroupDialogIds.contains(-1234567890L));
        assertTrue(c.allowedGroupDialogIds.contains(-987654321L));
        assertTrue(c.allowedUserIds.contains(111111111L));
        assertTrue(c.allowedUserIds.contains(7012345678L));
    }

    @Test
    public void olderTamperedAndForeignEnvelopesAreIgnored() throws Exception {
        ILGRAMRemoteSync s = sync();
        s.accept(V3, null);
        s.accept(V2, null);
        assertEquals("downgrade ignored", 3, s.current().version);
        s.accept(TAMPERED, null);
        s.accept(WRONG_SIGNER, null);
        assertEquals(3, s.current().version);
        assertEquals(2, s.current().users.size());
    }

    @Test
    public void rejectedFirstEnvelopeLeavesNothingApplied() throws Exception {
        ILGRAMRemoteSync s = sync();
        s.accept(WRONG_SIGNER, null);
        assertNull(s.current());
        assertNull(s.properties());
    }

    @Test
    public void cacheSurvivesRestartAndIsReverified() throws Exception {
        sync().accept(V3, null);
        ILGRAMRemoteSync.dropInstanceForTesting();
        Properties bundled = new Properties();
        bundled.setProperty(ILGRAMRemoteSync.KEY_URL, "https://127.0.0.1:1/config");
        bundled.setProperty(ILGRAMRemoteSync.KEY_PUBLIC_KEY, PUB);
        bundled.setProperty(ILGRAMRemoteSync.KEY_AES_KEY, AES);
        ILGRAMRemoteSync s = ILGRAMRemoteSync.create(app, bundled);
        assertNotNull(s);
        assertNotNull(s.current());
        assertEquals(3, s.current().version);
    }

    @Test
    public void notConfiguredOrPlainHttpMeansOff() {
        Properties p = new Properties();
        assertNull(ILGRAMRemoteSync.create(app, p));
        p.setProperty(ILGRAMRemoteSync.KEY_URL, "http://example.com/config");
        p.setProperty(ILGRAMRemoteSync.KEY_PUBLIC_KEY, PUB);
        p.setProperty(ILGRAMRemoteSync.KEY_AES_KEY, AES);
        assertNull(ILGRAMRemoteSync.create(app, p));
    }

    @Test
    public void networkErrorKeepsCurrentList() throws Exception {
        ILGRAMRemoteSync s = sync();
        s.accept(V3, null);
        assertFalse(s.checkNow()); // nothing listens on 127.0.0.1:1
        assertEquals(3, s.current().version);
    }
}
