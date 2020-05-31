package com.automattic.simplenote.espressoUITestsHelpers;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions;

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
import static java.util.concurrent.TimeUnit.SECONDS;

public class EspressoAuthenticationHelpers {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public static void loginWithEmail() {
        clickOn(R.id.button_login);
        clickOn(R.id.button_email);
        BaristaSleepInteractions.sleep(2, SECONDS);
    }

    @Test
    public static void enterApp() {
        clickOn(R.id.button);
        BaristaSleepInteractions.sleep(2, SECONDS);
    }

    //taps on sign up/login button after entering email/pw, both buttons have the same ID

    @Test
    public static void enterEmailPassword(String email, String password) {
        writeTo(R.id.input_email, email);
        writeTo(R.id.input_password, password);
        BaristaSleepInteractions.sleep(2, SECONDS);
    }

    @Test
    public static void signUp() {
        clickOn(R.id.button_signup);
        BaristaSleepInteractions.sleep(2, SECONDS);
    }

    @Test
    public static void logOut() {
        openDrawer();
        clickListItem(R.id.design_navigation_view, 3);
        BaristaSleepInteractions.sleep(2, SECONDS);
        swipeToBottom();
        clickOn(R.string.log_out);
        try {
            onView(withText(R.string.unsynced_notes_message)).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform((click()));
        } catch (NoMatchingViewException e) {
        }
    }


}