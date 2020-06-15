package com.automattic.simplenote.smoke.test;

import androidx.test.rule.ActivityTestRule;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.smoke.data.DataProvider;
import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;
import com.automattic.simplenote.smoke.utils.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRunner {
    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    IntroPage introPage = new IntroPage();
    LoginPage loginPage;
    MainPage mainPage = new MainPage();

    @Before
    public void setUp() {
        TestUtils.giveMeABreak();
        TestUtils.logoutIfNecessary();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLogin() {
        LoginPage loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);
        mainPage.isOpened();
    }

    @Test
    public void testLogout() {
        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        mainPage.isOpened();
        mainPage.logout();
    }

    @Test
    public void testLoginWithWrongPassword() {
        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_WRONG_PASSWORD);
        loginPage.checkLoginFailedMessage();
    }
}
