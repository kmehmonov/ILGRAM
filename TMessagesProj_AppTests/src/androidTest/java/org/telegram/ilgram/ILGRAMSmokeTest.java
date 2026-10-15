/*
 * ILGRAM UI smoke test on an emulator: starts the real LaunchActivity, feeds it hostile deep links
 * (proxy, invite, mini app, external web) and checks the app stays alive. Screenshots are saved to
 * <external files>/ilgram-screens and uploaded by CI so a human can look at them.
 */
package org.telegram.ilgram;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ILGRAMSmokeTest {

    private void screenshot(String name) {
        try {
            Instrumentation inst = InstrumentationRegistry.getInstrumentation();
            Bitmap bmp = inst.getUiAutomation().takeScreenshot();
            if (bmp == null) {
                return;
            }
            Context ctx = inst.getTargetContext();
            File dir = new File(ctx.getExternalFilesDir(null), "ilgram-screens");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Throwable ignore) {
        }
    }

    private Activity resumedActivity() {
        AtomicReference<Activity> ref = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            if (!resumed.isEmpty()) {
                ref.set(resumed.iterator().next());
            }
        });
        return ref.get();
    }

    private void sendView(String url) throws InterruptedException {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setClass(ctx, LaunchActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
        Thread.sleep(2500);
    }

    @Test
    public void launch_and_hostileDeepLinks_doNotCrash() throws Exception {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        Context ctx = inst.getTargetContext();
        Intent launch = new Intent(ctx, LaunchActivity.class);
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(launch);

        Activity activity = null;
        for (int i = 0; i < 40 && activity == null; i++) {
            Thread.sleep(500);
            activity = resumedActivity();
        }
        assertNotNull("ILGRAM started", activity);
        Thread.sleep(4000);
        screenshot("01-start");

        String[] hostile = {
                "tg://proxy?server=1.2.3.4&port=443&secret=00",
                "https://t.me/joinchat/AAAAAAAAAAAAAAAA",
                "https://t.me/somebot?startapp=x",
                "tg://resolve?domain=somebot&startattach",
                "https://t.me/addlist/abcdef",
                "https://example.com/",
        };
        int n = 2;
        for (String url : hostile) {
            sendView(url);
            screenshot(String.format("%02d-deeplink", n++));
            assertNotNull("app still alive after " + url, resumedActivity());
        }
        assertTrue(true);
    }
}
