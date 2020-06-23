package com.automattic.simplenote.smoke.utils;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class TestUtils {

    private static final int SHORT_PERIOD = 2000;

    public static void idleFor(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception ex) {
            // do nothing
        }
    }

    public static void idleForAShortPeriod() {
        idleFor(SHORT_PERIOD);
    }

    // TODO This method should be moved to non static context
    public static void logoutIfNecessary() {
        if (!new IntroPage().isOpened() && !new LoginPage().isLoginFailed()) {
            new MainPage().logout();
            TestUtils.idleForAShortPeriod();
        }
    }

    private static final int LAUNCH_TIMEOUT = 5000;

    public static void relaunchSimplenote() {
        // Taken straight out of:
        // https://developer.android.com/training/testing/ui-testing/uiautomator-testing#java
        // Initialize UiDevice instance
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Start from the home screen
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        // Launch the app
        Context context = ApplicationProvider.getApplicationContext();
        String packageName = context.getPackageName();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), LAUNCH_TIMEOUT);
    }
}
