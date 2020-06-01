package com.automattic.simplenote.espressoUITests;


import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.enterApp;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.enterEmailPassword;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.logOut;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithEmail;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.signUp;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static java.lang.StrictMath.random;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AuthenticationTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void signUpWithCredentialsAndLogout() {
        signUp();
        enterEmailPassword(random() + "@gmail.com", "testespresso");
        enterApp();
        logOut();
    }

    @Test
    public void loginWithValidCredentialsLogout() {
        loginWithEmail();
        enterEmailPassword(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        enterApp();
        logOut();
    }

    @Test
    public void loginWithInvalidCredentialsVerifyAlert() {
        loginWithEmail();
        enterEmailPassword("test.espresso.90890@gmail.com","testespresso");
        enterApp();
        assertContains(R.string.simperium_dialog_message_login);
    }

    @Test
    public void loginWithValidCredentialsAfterUsingWrongCredentials() {
        loginWithEmail();
        enterEmailPassword("test.espresso.90890@gmail.com","testespresso");
        enterApp();
        assertContains(R.string.simperium_dialog_message_login);
        onView(withId(android.R.id.button1)).perform((click()));
        enterEmailPassword(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        enterApp();
        logOut();
    }

}

