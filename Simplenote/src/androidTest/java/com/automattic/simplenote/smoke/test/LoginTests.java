package com.automattic.simplenote.smoke.test;

import androidx.test.rule.ActivityTestRule;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.smoke.data.DataProvider;
import com.automattic.simplenote.smoke.data.SignUpDataProvider;
import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;
import com.automattic.simplenote.smoke.utils.TestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LoginTests {
    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class, true, true);

    private static String email;
    private static String password;

    @Before
    public void init() {
        TestUtils.logoutIfNecessary();
        if (email == null || password == null) {

            email = SignUpDataProvider.generateEmail();
            password = SignUpDataProvider.generatePassword();

            new IntroPage()
                    .openSignUp()
                    .signUp(email, password)
                    .logout(email);
        }
    }

    @Test
    public void testLogin() {

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .isOpened()
                .logout(email);
    }

    @Test
    public void testLogout() {

        if (!TestUtils.logoutIfNecessary()) {
            new IntroPage()
                    .goToLoginWithEmail()
                    .login(email, password);

            new MainPage()
                    .isOpened()
                    .logout(email);
        }
    }

    @Test
    public void testLoginWithWrongPassword() {
        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_WRONG_PASSWORD);

        new LoginPage()
                .checkLoginFailedMessage();
    }
}
