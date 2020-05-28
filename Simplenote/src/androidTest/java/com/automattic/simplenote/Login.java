package com.automattic.simplenote;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.automattic.simplenote.utils.EspressoUITests.logOut;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithCredentials;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithInvalidCredentials;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithValidCredentials;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class Login {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void loginWithValidCredentialsLogout() throws InterruptedException {
        loginWithCredentials();
        loginWithValidCredentials();
        logOut();
    }

    @Test
    public void loginWithInvalidCredentialsVerifyAlert() throws InterruptedException {
        loginWithCredentials();
        loginWithInvalidCredentials();
        assertContains(R.string.simperium_dialog_message_login);
    }

    @Test
    public void loginWithValidCredentialsAfterUsingWrongCredentials() throws InterruptedException {
        loginWithCredentials();
        loginWithInvalidCredentials();
        assertContains(R.string.simperium_dialog_message_login);
        onView(withId(android.R.id.button1)).perform((click()));
        loginWithValidCredentials();
    }

}

