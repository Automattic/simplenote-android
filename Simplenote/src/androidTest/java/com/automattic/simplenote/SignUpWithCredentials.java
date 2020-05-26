package com.automattic.simplenote;


import com.automattic.simplenote.utils.EspressoUITests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo;
import static java.lang.Thread.sleep;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SignUpWithCredentials {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void signUpWithCredentialsAndLogout() throws InterruptedException {

        sleep(2000);

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
        EspressoUITests.logOut();
    }
}
