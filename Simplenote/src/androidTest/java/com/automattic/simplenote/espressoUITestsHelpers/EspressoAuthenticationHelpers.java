package com.automattic.simplenote.espressoUITestsHelpers;

import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoGeneralHelpers.swipeToBottom;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer;
import static com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.schibsted.spain.barista.interaction.BaristaListInteractions.clickListItem;
import static java.lang.Thread.sleep;

public class EspressoAuthenticationHelpers {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public static void loginWithCredentials() throws InterruptedException {
        clickOn(R.id.button_login);
        clickOn(R.id.button_email);
        sleep(2000);
    }

    @Test
    public static void loginWithValidCredentials() throws InterruptedException {
        writeTo(R.id.input_email, BuildConfig.TEST_USER_EMAIL);
        writeTo(R.id.input_password, BuildConfig.TEST_USER_PASSWORD);
        clickOn(R.id.button);
        sleep(2000);
    }

    @Test
    public static void loginWithInvalidCredentials() throws InterruptedException {
        writeTo(R.id.input_email, "test.espresso.00005@gmail.com");
        writeTo(R.id.input_password, "*-Re7]J4Ux8q)g?X");
        clickOn(R.id.button);
        sleep(2000);
    }

    @Test
    public static void logOut() throws InterruptedException {
        openDrawer();
        clickListItem(R.id.design_navigation_view, 3);
        sleep(2000);
        swipeToBottom();
        clickOn(R.string.log_out);
        try {
            onView(withText(R.string.unsynced_notes_message)).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform((click()));
        } catch (NoMatchingViewException e) {
        }
    }


}