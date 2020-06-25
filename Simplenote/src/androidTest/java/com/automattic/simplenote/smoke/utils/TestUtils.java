package com.automattic.simplenote.smoke.utils;

import com.automattic.simplenote.smoke.data.DataProvider;
import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;

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

    public static boolean logoutIfNecessary() {
        if (!new IntroPage().isOpened() && !new LoginPage().isLoginFailed()) {
            new MainPage()
                    .logout(DataProvider.UNKNOWN_EMAIL);
            TestUtils.idleForAShortPeriod();
            return true;
        }
        return false;
    }
}
