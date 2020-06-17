package com.automattic.simplenote.smoke.utils;

import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;

public class TestUtils {

    private static final int GIVE_ME_A_BREAK_TIME = 2000;

    public static void idleFor(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception ex) {
            // do nothing
        }
    }

    public static void giveMeABreak() {
        idleFor(GIVE_ME_A_BREAK_TIME);
    }

    // TODO This method should be moved to non static context
    public static void logoutIfNecessary() {
        if (!new IntroPage().isOpened() && !new LoginPage().isLoginFailed()) {
            new MainPage().logout();
            TestUtils.giveMeABreak();
        }
    }
}
