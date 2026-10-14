/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 *
 * ILGRAM: in student mode this service runs in the foreground (low-importance notification)
 * so that the Telegram connection stays alive and messages arrive while the app is closed.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import org.telegram.ui.LaunchActivity;

public class NotificationsService extends Service {

    private static final String ILGRAM_CHANNEL_ID = "ilgram_keepalive";
    private static final int ILGRAM_NOTIFICATION_ID = 0x16a7;

    @Override
    public void onCreate() {
        super.onCreate();
        if (org.telegram.ilgram.ILGRAMPolicy.isActive()) {
            startIlgramForeground();
        }
        ApplicationLoader.postInitApplication();
    }

    private void startIlgramForeground() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(ILGRAM_CHANNEL_ID) == null) {
                    NotificationChannel channel = new NotificationChannel(ILGRAM_CHANNEL_ID, "ILGRAM ulanish", NotificationManager.IMPORTANCE_MIN);
                    channel.setDescription("Xabarlarni fon rejimida qabul qilish uchun");
                    channel.setShowBadge(false);
                    nm.createNotificationChannel(channel);
                }
            }
            Intent open = new Intent(this, LaunchActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new NotificationCompat.Builder(this, ILGRAM_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle("ILGRAM")
                    .setContentText("Xabarlar fon rejimida qabul qilinmoqda")
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .setContentIntent(pi)
                    .build();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(ILGRAM_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING);
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(ILGRAM_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(ILGRAM_NOTIFICATION_ID, n);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (org.telegram.ilgram.ILGRAMPolicy.isActive() || preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }
}
