package com.automattic.simplenote.espressoUITests;


import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.logOut;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.login;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithCredentials;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo;
import static java.lang.Thread.sleep;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AuthenticationTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void signUpWithCredentialsAndLogout() throws InterruptedException {

        //generate random string

        char[] chars1 = "ABCDEF012GHIJKL345MNOPQR678STUVWXYZ9".toCharArray();
        StringBuilder sb1 = new StringBuilder();
        Random random1 = new Random();
        for (int i = 0; i < 6; i++) {
            char c1 = chars1[random1.nextInt(chars1.length)];
            sb1.append(c1);
        }
        String random_string = sb1.toString();

        clickOn(R.id.button_signup);
        writeTo(R.id.input_email, (random_string + "@gmail.com"));
        writeTo(R.id.input_password, "*-Re7]J4Ux8q)g?X");
        clickOn(R.id.button);
        sleep(2000);
        logOut();
    }

    @Test
    public void loginWithValidCredentialsLogout() throws InterruptedException {
        loginWithCredentials();
        login(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        logOut();
    }

    @Test
    public void loginWithInvalidCredentialsVerifyAlert() throws InterruptedException {
        loginWithCredentials();
        login("test.espresso.90890@gmail.com","testespresso");
        BaristaVisibilityAssertions.assertContains(R.string.simperium_dialog_message_login);
    }

    @Test
    public void loginWithValidCredentialsAfterUsingWrongCredentials() throws InterruptedException {
        loginWithCredentials();
        login("test.espresso.90890@gmail.com","testespresso");
        assertContains(R.string.simperium_dialog_message_login);
        onView(withId(android.R.id.button1)).perform((click()));
        login(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        logOut();
    }

}

