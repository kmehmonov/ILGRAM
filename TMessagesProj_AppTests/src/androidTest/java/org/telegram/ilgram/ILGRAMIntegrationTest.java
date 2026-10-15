/*
 * ILGRAM on-device integration tests (run on an Android emulator in CI, no Telegram login needed).
 *
 * They exercise the real Telegram classes with ILGRAM hooks inside the app process:
 * chat-list filtering in MessagesController, the navigation guard, links, notifications,
 * settings and web views. Test numbers in method names refer to section 17 of the ILGRAM spec.
 */
package org.telegram.ilgram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import androidx.collection.LongSparseArray;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionIntroActivity;
import org.telegram.ui.CallLogActivity;
import org.telegram.ui.ChannelCreateActivity;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ContactsActivity;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.GroupCreateActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.TopicsFragment;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ILGRAMIntegrationTest {

    private static final int ACCOUNT = 0;

    // Allowed
    private static final long ALLOWED_SUPERGROUP = 1234567890L;   // Bot API -1001234567890
    private static final long ALLOWED_BASIC_GROUP = 987654321L;   // Bot API -987654321
    private static final long ALLOWED_TEACHER = 111111111L;
    // Not allowed
    private static final long FOREIGN_SUPERGROUP = 2222222222L;
    private static final long FOREIGN_LUCK_GROUP = 3333333333L;   // has "luck" in the name, still not allowed
    private static final long FOREIGN_CHANNEL = 4444444444L;
    private static final long FOREIGN_USER = 555555555L;
    private static final long FOREIGN_BOT = 666666666L;
    private static final long SERVICE = 777000L;

    private Instrumentation instrumentation;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        Properties p = new Properties();
        p.setProperty("student_mode_enabled", "true");
        p.setProperty("allowed_group_ids", "-100" + ALLOWED_SUPERGROUP + ", -" + ALLOWED_BASIC_GROUP);
        p.setProperty("allowed_user_ids", String.valueOf(ALLOWED_TEACHER));
        ILGRAMPolicy.setConfigForTesting(ILGRAMRules.parse(p, null));
    }

    private void onMain(Runnable r) {
        instrumentation.runOnMainSync(r);
    }

    // ------------------------------------------------------------------ fixtures

    private static TLRPC.Chat supergroup(long id, String title) {
        TLRPC.TL_channel c = new TLRPC.TL_channel();
        c.id = id;
        c.title = title;
        c.megagroup = true;
        c.access_hash = 1;
        return c;
    }

    private static TLRPC.Chat broadcast(long id, String title) {
        TLRPC.TL_channel c = new TLRPC.TL_channel();
        c.id = id;
        c.title = title;
        c.megagroup = false;
        c.broadcast = true;
        c.access_hash = 1;
        return c;
    }

    private static TLRPC.Chat basicGroup(long id, String title) {
        TLRPC.TL_chat c = new TLRPC.TL_chat();
        c.id = id;
        c.title = title;
        return c;
    }

    private static TLRPC.User user(long id, String name, boolean bot) {
        TLRPC.TL_user u = new TLRPC.TL_user();
        u.id = id;
        u.first_name = name;
        u.bot = bot;
        u.access_hash = 1;
        return u;
    }

    private static TLRPC.Dialog dialog(long dialogId) {
        TLRPC.TL_dialog d = new TLRPC.TL_dialog();
        d.id = dialogId;
        if (dialogId > 0) {
            d.peer = new TLRPC.TL_peerUser();
            d.peer.user_id = dialogId;
        } else {
            d.peer = new TLRPC.TL_peerChannel();
            d.peer.channel_id = -dialogId;
        }
        d.notify_settings = new TLRPC.TL_peerNotifySettings();
        d.top_message = 1;
        return d;
    }

    /** Loads a realistic mix of dialogs into the real MessagesController and sorts them. */
    private MessagesController loadDialogs(boolean archiveTeacher) {
        final MessagesController mc = MessagesController.getInstance(ACCOUNT);
        onMain(() -> {
            mc.putChat(supergroup(ALLOWED_SUPERGROUP, "9-A sinf"), false);
            mc.putChat(basicGroup(ALLOWED_BASIC_GROUP, "Matematika to'garagi"), false);
            mc.putChat(supergroup(FOREIGN_SUPERGROUP, "Random chat"), false);
            mc.putChat(supergroup(FOREIGN_LUCK_GROUP, "LUCK fans (not school)"), false);
            mc.putChat(broadcast(FOREIGN_CHANNEL, "Some channel"), false);
            mc.putUser(user(ALLOWED_TEACHER, "Teacher", false), false);
            mc.putUser(user(FOREIGN_USER, "Stranger", false), false);
            mc.putUser(user(FOREIGN_BOT, "SomeBot", true), false);
            mc.putUser(user(SERVICE, "Telegram", false), false);

            ArrayList<TLRPC.Dialog> all = mc.getAllDialogsInternal();
            all.clear();
            mc.dialogs_dict.clear();
            long[] ids = {-ALLOWED_SUPERGROUP, -ALLOWED_BASIC_GROUP, -FOREIGN_SUPERGROUP, -FOREIGN_LUCK_GROUP,
                    -FOREIGN_CHANNEL, ALLOWED_TEACHER, FOREIGN_USER, FOREIGN_BOT, SERVICE};
            for (long id : ids) {
                TLRPC.Dialog d = dialog(id);
                if (archiveTeacher && id == ALLOWED_TEACHER) {
                    d.folder_id = 1;
                }
                all.add(d);
                mc.dialogs_dict.put(id, d);
            }
            TLRPC.TL_dialogFolder archive = new TLRPC.TL_dialogFolder();
            archive.folder = new TLRPC.TL_folder();
            archive.folder.id = 1;
            archive.id = DialogObjectCompat.makeFolderDialogId(1);
            all.add(archive);
            mc.sortDialogs(new LongSparseArray<>());
        });
        return mc;
    }

    private static boolean contains(ArrayList<TLRPC.Dialog> list, long id) {
        for (TLRPC.Dialog d : list) {
            if (d.id == id) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ chat list (tests 2, 3, 4, 5, 6)

    @Test
    public void t02_t03_chatList_showsOnlyAllowedChats() {
        MessagesController mc = loadDialogs(false);
        ArrayList<TLRPC.Dialog> main = mc.getDialogs(0);
        assertTrue("allowed supergroup visible", contains(main, -ALLOWED_SUPERGROUP));
        assertTrue("allowed basic group visible", contains(main, -ALLOWED_BASIC_GROUP));
        assertTrue("allowed teacher visible", contains(main, ALLOWED_TEACHER));
        assertFalse("foreign group hidden", contains(main, -FOREIGN_SUPERGROUP));
        assertFalse("'luck' in the name does not grant access", contains(main, -FOREIGN_LUCK_GROUP));
        assertFalse("channel hidden", contains(main, -FOREIGN_CHANNEL));
        assertFalse("foreign user hidden", contains(main, FOREIGN_USER));
        assertFalse("bot hidden", contains(main, FOREIGN_BOT));
        for (TLRPC.Dialog d : main) {
            assertFalse("archive folder row hidden", d instanceof TLRPC.TL_dialogFolder);
        }
        // Underlying data is untouched (no deletion / falsification of server data)
        assertTrue(mc.dialogs_dict.get(-FOREIGN_SUPERGROUP) != null);
        assertEquals(10, mc.getAllDialogsInternal().size());
        // UI getter is filtered
        assertFalse(contains(mc.getAllDialogs(), FOREIGN_USER));
    }

    @Test
    public void t04_renamedGroup_staysAllowedById() {
        MessagesController mc = loadDialogs(false);
        onMain(() -> {
            TLRPC.Chat renamed = supergroup(ALLOWED_SUPERGROUP, "Completely new title");
            mc.putChat(renamed, false);
            mc.sortDialogs(new LongSparseArray<>());
        });
        assertTrue(contains(mc.getDialogs(0), -ALLOWED_SUPERGROUP));
    }

    @Test
    public void archivedAllowedChat_staysVisible() {
        MessagesController mc = loadDialogs(true);
        assertTrue("allowed chat archived by the student is still in the main list", contains(mc.getDialogs(0), ALLOWED_TEACHER));
    }

    @Test
    public void t05_t06_openChat_onlyAllowed() {
        assertTrue(canPresent(chat("user_id", ALLOWED_TEACHER)));
        assertFalse(canPresent(chat("user_id", FOREIGN_USER)));
        assertTrue(canPresent(chat("chat_id", ALLOWED_SUPERGROUP)));
        assertFalse(canPresent(chat("chat_id", FOREIGN_SUPERGROUP)));
        assertFalse(canPresent(chat("chat_id", FOREIGN_CHANNEL)));
        assertFalse(canPresent(chat("user_id", FOREIGN_BOT)));
    }

    // ------------------------------------------------------------------ navigation (tests 8, 9, 18 and more)

    @Test
    public void t08_t09_profiles_ofForeignUsersAreBlocked() {
        assertFalse("avatar/username tap on a classmate", canPresent(profile("user_id", FOREIGN_USER)));
        assertTrue(canPresent(profile("user_id", ALLOWED_TEACHER)));
        assertTrue(canPresent(profile("chat_id", ALLOWED_SUPERGROUP)));
        assertFalse(canPresent(profile("chat_id", FOREIGN_CHANNEL)));
    }

    @Test
    public void t18_creationAndDiscoveryScreens_areBlocked() {
        assertFalse(canPresentNew(() -> new GroupCreateActivity(new Bundle())));
        assertFalse(canPresentNew(() -> new ChannelCreateActivity(new Bundle())));
        assertFalse(canPresentNew(() -> new ContactsActivity(new Bundle())));
        assertFalse(canPresentNew(() -> new CallLogActivity(new Bundle())));
        assertFalse(canPresentNew(() -> new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_QR_LOGIN)));
        Bundle archive = new Bundle();
        archive.putInt("folderId", 1);
        assertFalse("archive", canPresentNew(() -> new DialogsActivity(archive)));
        assertTrue("main list", canPresentNew(() -> new DialogsActivity(new Bundle())));
        Bundle topics = new Bundle();
        topics.putLong("chat_id", FOREIGN_SUPERGROUP);
        assertFalse(canPresentNew(() -> new TopicsFragment(topics)));
    }

    // ------------------------------------------------------------------ forward (test 10)

    @Test
    public void t10_forwardTargets_onlyAllowed() {
        MessagesController mc = loadDialogs(false);
        ArrayList<TLRPC.Dialog> fwd = mc.dialogsForward;
        assertTrue(contains(fwd, -ALLOWED_SUPERGROUP));
        assertTrue(contains(fwd, ALLOWED_TEACHER));
        assertFalse(contains(fwd, FOREIGN_USER));
        assertFalse(contains(fwd, -FOREIGN_SUPERGROUP));
        assertFalse(contains(fwd, -FOREIGN_CHANNEL));
        assertFalse("service notifications are read-only", contains(fwd, SERVICE));
        assertFalse(ILGRAMPolicy.canForwardTo(ACCOUNT, FOREIGN_USER));
    }

    // ------------------------------------------------------------------ notifications (test 7)

    @Test
    public void t07_notifications_onlyForAllowedChats() {
        assertTrue(ILGRAMPolicy.shouldShowNotification(ACCOUNT, -ALLOWED_SUPERGROUP));
        assertTrue(ILGRAMPolicy.shouldShowNotification(ACCOUNT, ALLOWED_TEACHER));
        assertFalse(ILGRAMPolicy.shouldShowNotification(ACCOUNT, FOREIGN_USER));
        assertFalse(ILGRAMPolicy.shouldShowNotification(ACCOUNT, -FOREIGN_CHANNEL));
    }

    // ------------------------------------------------------------------ links / browser / mini apps (11-15, 29)

    @Test
    public void t11_t12_t29_links() {
        assertEquals(ILGRAMRules.LinkDecision.BLOCK, ILGRAMPolicy.classifyUrl("https://t.me/somebot?start=1"));
        assertEquals(ILGRAMRules.LinkDecision.BLOCK, ILGRAMPolicy.classifyUrl("https://t.me/joinchat/AAAA"));
        assertEquals(ILGRAMRules.LinkDecision.BLOCK, ILGRAMPolicy.classifyUrl("tg://proxy?server=1.1.1.1&port=1&secret=00"));
        assertEquals(ILGRAMRules.LinkDecision.ALLOW_INTERNAL, ILGRAMPolicy.classifyUrl("https://t.me/c/1234567890/5"));
        // Channel link resolves internally, but the navigation guard refuses the foreign channel itself
        assertFalse(canPresent(chat("chat_id", FOREIGN_CHANNEL)));
    }

    @Test
    public void t13_t14_webViews_disabled() {
        assertFalse(ILGRAMPolicy.isWebViewAllowed());
    }

    @Test
    public void t15_externalLink_neverStartsABrowser() throws Exception {
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addDataScheme("http");
        filter.addDataScheme("https");
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(filter, null, true);
        try {
            final Context ctx = ApplicationLoader.applicationContext;
            onMain(() -> Browser.openUrl(ctx, Uri.parse("https://example.com/page")));
            onMain(() -> Browser.openUrl(ctx, "http://example.org", true));
            Thread.sleep(1500);
            assertEquals("no browser activity was launched", 0, monitor.getHits());
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    // ------------------------------------------------------------------ settings / accounts (16, 17)

    @Test
    public void t16_t17_settings_simplified() {
        assertFalse(ILGRAMPolicy.isSettingVisible(ILGRAMRules.SETTING_ACCOUNTS));
        assertFalse(ILGRAMPolicy.isSettingVisible(ILGRAMRules.SETTING_FOLDERS));
        assertFalse(ILGRAMPolicy.isSettingVisible(ILGRAMRules.SETTING_PROXY));
        assertFalse(ILGRAMPolicy.isSettingVisible(ILGRAMRules.SETTING_PREMIUM));
        assertTrue(ILGRAMPolicy.isSettingVisible(ILGRAMRules.SETTING_LANGUAGE));
        assertTrue(ILGRAMPolicy.isSettingVisible(ILGRAMRules.SETTING_NOTIFICATIONS));
        assertTrue(ILGRAMPolicy.isSettingVisible(ILGRAMRules.SETTING_PRIVACY));
        assertFalse(ILGRAMPolicy.isMultiAccountEnabled());
        assertFalse(ILGRAMPolicy.isGlobalSearchEnabled());
    }

    // ------------------------------------------------------------------ branding / config

    @Test
    public void branding_appNameIsIlgram() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("ILGRAM", ctx.getString(R.string.AppName));
    }

    @Test
    public void bundledConfig_isReadable() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Properties p = new Properties();
        p.load(ctx.getAssets().open(ILGRAMPolicy.ASSET_CONFIG));
        assertNotNull(p.getProperty(ILGRAMRules.KEY_STUDENT_MODE));
    }

    @Test
    public void studentModeOff_restoresNormalTelegram() {
        Properties p = new Properties();
        p.setProperty("student_mode_enabled", "false");
        ILGRAMPolicy.setConfigForTesting(ILGRAMRules.parse(p, null));
        try {
            MessagesController mc = loadDialogs(false);
            assertTrue(contains(mc.getDialogs(0), FOREIGN_USER));
            assertTrue(canPresent(chat("user_id", FOREIGN_USER)));
        } finally {
            setUp();
        }
    }

    // ------------------------------------------------------------------ helpers

    private interface FragmentFactory {
        BaseFragment create();
    }

    private Bundle args(String key, long value) {
        Bundle b = new Bundle();
        b.putLong(key, value);
        return b;
    }

    private FragmentFactory chat(String key, long id) {
        return () -> new ChatActivity(args(key, id));
    }

    private FragmentFactory profile(String key, long id) {
        return () -> new ProfileActivity(args(key, id));
    }

    private boolean canPresent(FragmentFactory f) {
        return canPresentNew(f);
    }

    private boolean canPresentNew(FragmentFactory f) {
        AtomicReference<Boolean> result = new AtomicReference<>();
        onMain(() -> result.set(ILGRAMPolicy.canPresentFragment(f.create())));
        return result.get();
    }

    /** Small shim so the test does not depend on DialogObject internals of a given Telegram version. */
    private static final class DialogObjectCompat {
        static long makeFolderDialogId(int folderId) {
            return org.telegram.messenger.DialogObject.makeFolderDialogId(folderId);
        }
    }
}
