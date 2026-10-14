/*
 * ILGRAM — restricted-UI Telegram client for Istiqbol LUCK / Istiqbol LIGHT schools.
 * Based on Telegram for Android (https://github.com/DrKLO/Telegram), GNU GPL v2 or later.
 *
 * ILGRAMPolicy is the single place every UI restriction asks. Screens never implement their own
 * allow/deny logic; they call one of the methods below.
 *
 * Scope reminder: these are restrictions of this app's user interface only. The Telegram account is
 * not restricted on Telegram's servers; messages are never deleted, altered or copied elsewhere.
 */
package org.telegram.ilgram;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionIntroActivity;
import org.telegram.ui.CallLogActivity;
import org.telegram.ui.CameraScanActivity;
import org.telegram.ui.ChannelCreateActivity;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.MediaActivity;
import org.telegram.ui.ContactsActivity;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.FilterCreateActivity;
import org.telegram.ui.FiltersSetupActivity;
import org.telegram.ui.GroupCreateActivity;
import org.telegram.ui.GroupCreateFinalActivity;
import org.telegram.ui.InviteContactsActivity;
import org.telegram.ui.LinkEditActivity;
import org.telegram.ui.LoginActivity;
import org.telegram.ui.ManageLinksActivity;
import org.telegram.ui.PaymentFormActivity;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.ProxyListActivity;
import org.telegram.ui.ProxySettingsActivity;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.TopicsFragment;
import org.telegram.ui.WebviewActivity;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class ILGRAMPolicy {

    public static final String ASSET_CONFIG = "ilgram.properties";
    public static final String BLOCKED_MESSAGE = "ILGRAM: bu bo'lim maktab qoidalariga ko'ra yopilgan.";

    private static volatile ILGRAMRules.Config config = ILGRAMRules.Config.lockedDown();
    private static volatile boolean initialized;
    private static long lastToastTime;

    private ILGRAMPolicy() {
    }

    // =====================================================================================
    // Configuration
    // =====================================================================================

    /** Called from ApplicationLoader.onCreate. Safe to call more than once. */
    public static synchronized void init(Context context) {
        if (initialized || context == null) {
            return;
        }
        initialized = true;
        final Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        reload(app);
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                // Device Owner / MDM can change the managed configuration at runtime.
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctx, Intent intent) {
                        reload(app);
                        AndroidUtilities.runOnUIThread(ILGRAMPolicy::onConfigChanged);
                    }
                };
                IntentFilter filter = new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);
                if (Build.VERSION.SDK_INT >= 33) {
                    app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    app.registerReceiver(receiver, filter);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    private static void reload(Context app) {
        Properties base = new Properties();
        try (InputStream in = app.getAssets().open(ASSET_CONFIG)) {
            base.load(new java.io.InputStreamReader(in, "UTF-8"));
        } catch (Throwable e) {
            FileLog.e(e);
        }
        Properties managed = new Properties();
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                RestrictionsManager rm = (RestrictionsManager) app.getSystemService(Context.RESTRICTIONS_SERVICE);
                Bundle b = rm != null ? rm.getApplicationRestrictions() : null;
                if (b != null) {
                    for (String key : b.keySet()) {
                        Object v = b.get(key);
                        if (v instanceof String[]) {
                            managed.setProperty(key, android.text.TextUtils.join(",", (String[]) v));
                        } else if (v != null) {
                            managed.setProperty(key, String.valueOf(v));
                        }
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        config = ILGRAMRules.parse(base, managed);
        FileLog.d("ILGRAM config loaded: " + config);
    }

    private static void onConfigChanged() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                continue;
            }
            try {
                MessagesController.getInstance(a).sortDialogs(null);
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.dialogsNeedReload);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    public static ILGRAMRules.Config config() {
        if (!initialized && ApplicationLoader.applicationContext != null) {
            init(ApplicationLoader.applicationContext);
        }
        return config;
    }

    /** True when the restricted student UI is active. */
    public static boolean isActive() {
        return config().studentModeEnabled;
    }

    // =====================================================================================
    // Dialogs
    // =====================================================================================

    /** Supergroups that an allowed basic group was upgraded to keep the permission automatically. */
    private static Set<Long> migratedAllowedGroups(int account) {
        Set<Long> out = null;
        MessagesController mc = MessagesController.getInstance(account);
        for (Long groupDialogId : config().allowedGroupDialogIds) {
            TLRPC.Chat chat = mc.getChat(-groupDialogId);
            if (chat != null && chat.migrated_to != null && chat.migrated_to.channel_id != 0) {
                if (out == null) {
                    out = new HashSet<>();
                }
                out.add(-chat.migrated_to.channel_id);
            }
        }
        return out;
    }

    /** isAllowedGroup(chatId) / isAllowedUser(userId) / canOpenDialog(dialogId) in one call. */
    public static boolean canOpenDialog(int account, long dialogId) {
        ILGRAMRules.Config c = config();
        if (!c.studentModeEnabled) {
            return true;
        }
        long peer = dialogId;
        boolean secret = false;
        if (DialogObject.isEncryptedDialog(dialogId)) {
            secret = true;
            TLRPC.EncryptedChat enc = MessagesController.getInstance(account).getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
            if (enc == null) {
                return false;
            }
            peer = enc.user_id;
        }
        long self = UserConfig.getInstance(account).getClientUserId();
        Set<Long> extra = peer < 0 ? migratedAllowedGroups(account) : null;
        return ILGRAMRules.isDialogAllowed(c, peer, self, secret, extra);
    }

    public static boolean isAllowedGroup(int account, long chatId) {
        return canOpenDialog(account, -Math.abs(chatId));
    }

    public static boolean isAllowedUser(int account, long userId) {
        return userId > 0 && canOpenDialog(account, userId);
    }

    /** Chat list presentation: should this dialog row exist at all? */
    public static boolean isDialogVisible(int account, TLRPC.Dialog d) {
        if (!isActive()) {
            return true;
        }
        if (d == null || d instanceof TLRPC.TL_dialogFolder || d instanceof TLRPC.TL_dialogCommunity) {
            return false;
        }
        if (MessagesController.getInstance(account).isPromoDialog(d.id, false)) {
            return canOpenDialog(account, d.id) && !MessagesController.getInstance(account).isPromoDialog(d.id, true);
        }
        return canOpenDialog(account, d.id);
    }

    public static boolean canForwardTo(int account, long dialogId) {
        return canOpenDialog(account, dialogId) && !ILGRAMRules.isReadOnlyDialog(dialogId);
    }

    public static boolean shouldShowNotification(int account, long dialogId) {
        return canOpenDialog(account, dialogId);
    }

    public static boolean isSettingVisible(String settingKey) {
        return ILGRAMRules.isSettingVisible(config(), settingKey);
    }

    public static boolean isGlobalSearchEnabled() {
        return !isActive() || config().globalSearchEnabled;
    }

    public static boolean isMultiAccountEnabled() {
        return !isActive() || config().multiAccountEnabled;
    }

    // =====================================================================================
    // Navigation
    // =====================================================================================

    /**
     * canNavigate(destination): called by ActionBarLayout before any fragment is shown
     * (presentFragment, addFragmentToStack, sheets). Returns false to cancel the navigation.
     */
    public static boolean canPresentFragment(BaseFragment fragment) {
        if (fragment == null || !isActive()) {
            return true;
        }
        final int account = fragment.getCurrentAccount();
        final Bundle args = fragment.getArguments();
        final long self = UserConfig.getInstance(account).getClientUserId();

        if (fragment instanceof ChatActivity) {
            if (args == null) {
                return false;
            }
            if (args.containsKey("attach_bot") || args.containsKey("attach_bot_start_command")) {
                // never auto-open mini apps through a chat
                args.remove("attach_bot");
                args.remove("attach_bot_start_command");
            }
            long chatId = args.getLong("chat_id", 0);
            long userId = args.getLong("user_id", 0);
            int encId = args.getInt("enc_id", 0);
            if (encId != 0) {
                return canOpenDialog(account, DialogObject.makeEncryptedDialogId(encId));
            }
            if (chatId != 0) {
                return canOpenDialog(account, -chatId);
            }
            if (userId != 0) {
                return canOpenDialog(account, userId);
            }
            return false;
        }
        if (fragment instanceof ProfileActivity) {
            if (args == null) {
                return false;
            }
            long userId = args.getLong("user_id", 0);
            long chatId = args.getLong("chat_id", 0);
            if (userId != 0 && userId == self) {
                return true; // own profile / settings
            }
            if (args.getBoolean("my_profile", false)) {
                return userId == 0 || userId == self;
            }
            if (chatId != 0) {
                return canOpenDialog(account, -chatId);
            }
            if (userId != 0) {
                return canOpenDialog(account, userId);
            }
            return false; // vcard previews and other profile variants
        }
        if (fragment instanceof TopicsFragment) {
            return args != null && canOpenDialog(account, -Math.abs(args.getLong("chat_id", 0)));
        }
        if (fragment instanceof MediaActivity) {
            if (args == null) {
                return false;
            }
            long dialogId = args.getLong("dialog_id", 0);
            return dialogId == 0 || dialogId == self || canOpenDialog(account, dialogId);
        }
        if (fragment instanceof DialogsActivity) {
            if (args == null) {
                return true;
            }
            // archive (folder 1) and communities are not part of the student UI
            return args.getInt("folderId", 0) == 0 && args.getLong("community_id", 0) == 0;
        }
        if (fragment instanceof LoginActivity) {
            return isMultiAccountEnabled() || UserConfig.getActivatedAccountsCount() == 0;
        }
        if (fragment instanceof ActionIntroActivity) {
            int type = ((ActionIntroActivity) fragment).getActionType();
            return type != ActionIntroActivity.ACTION_TYPE_QR_LOGIN && type != ActionIntroActivity.ACTION_TYPE_CHANNEL_CREATE;
        }
        return !(fragment instanceof ContactsActivity
                || fragment instanceof InviteContactsActivity
                || fragment instanceof GroupCreateActivity
                || fragment instanceof GroupCreateFinalActivity
                || fragment instanceof ChannelCreateActivity
                || fragment instanceof CallLogActivity
                || fragment instanceof FiltersSetupActivity
                || fragment instanceof FilterCreateActivity
                || fragment instanceof ProxyListActivity
                || fragment instanceof ProxySettingsActivity
                || fragment instanceof PremiumPreviewFragment
                || fragment instanceof StarsIntroActivity
                || fragment instanceof WebviewActivity
                || fragment instanceof CameraScanActivity
                || fragment instanceof PaymentFormActivity
                || fragment instanceof ManageLinksActivity
                || fragment instanceof LinkEditActivity);
    }

    /** Convenience for call sites: checks and, if blocked, tells the user. */
    public static boolean checkPresent(BaseFragment fragment) {
        if (canPresentFragment(fragment)) {
            return true;
        }
        FileLog.d("ILGRAM blocked navigation to " + fragment.getClass().getName());
        showBlocked(fragment.getParentActivity() != null ? fragment.getParentActivity() : ApplicationLoader.applicationContext);
        return false;
    }

    // =====================================================================================
    // Links / browser / mini apps
    // =====================================================================================

    public static ILGRAMRules.LinkDecision classifyUrl(String url) {
        return ILGRAMRules.classifyLink(config(), url);
    }

    /** Called at the top of Browser.openUrl and the external VIEW-intent handler. */
    public static boolean canOpenUrl(Context context, Uri uri) {
        if (!isActive()) {
            return true;
        }
        if (uri != null && classifyUrl(uri.toString()) != ILGRAMRules.LinkDecision.BLOCK) {
            return true;
        }
        showBlocked(context);
        return false;
    }

    /** In-app browser, Telegram Mini Apps / Web Apps and bot web views. */
    public static boolean isWebViewAllowed() {
        return !isActive() || config().browserEnabled;
    }

    // =====================================================================================
    // Feedback
    // =====================================================================================

    public static void showBlocked(Context context) {
        final Context ctx = context != null ? context : ApplicationLoader.applicationContext;
        if (ctx == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            long now = SystemClock.elapsedRealtime();
            if (now - lastToastTime < 1500) {
                return;
            }
            lastToastTime = now;
            try {
                Toast.makeText(ctx, BLOCKED_MESSAGE, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignore) {
            }
        });
    }
}
