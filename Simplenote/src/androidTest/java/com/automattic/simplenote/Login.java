package com.automattic.simplenote;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.automattic.simplenote.utils.EspressoUITests.logOut;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithCredentials;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo;
import static java.lang.Thread.sleep;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class Login {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void loginWithCorrectCredentialsLogout() throws InterruptedException {
        loginWithCredentials();
        logOut();
    }

    @Test
    public void loginWithInvalidCredentials() throws InterruptedException {
        clickOn(R.id.button_login);
        clickOn(R.id.button_email);
        writeTo(R.id.input_email, "test.espresso.00005@gmail.com");
        writeTo(R.id.input_password, "*-Re7]J4Ux8q)g?X");
        clickOn(R.id.button);
        sleep(2000);
        assertContains(R.string.simperium_dialog_message_login);
    }
}

