package com.automattic.simplenote.smoke.test;

import androidx.test.rule.ActivityTestRule;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.smoke.data.DataProvider;
import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;

import org.junit.Rule;
import org.junit.Test;

public class TestRunner {
    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void testLogin() {

        MainPage mainPage = new MainPage();

        if (mainPage.isOpen()) {
            mainPage.logout();
        }

        LoginPage loginPage = new IntroPage().goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

    }
}
