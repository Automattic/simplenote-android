package com.automattic.simplenote.utils;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer;
import static com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.schibsted.spain.barista.interaction.BaristaListInteractions.clickListItem;
import static java.lang.Thread.sleep;
import static org.hamcrest.core.StringEndsWith.endsWith;

public class EspressoUITests {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public static void loginWithCredentials() throws InterruptedException {
        clickOn(R.id.button_login);
        clickOn(R.id.button_email);
        writeTo(R.id.input_email, "test.espresso.00001@gmail.com");
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
        sleep(2000);
        onView(withText(endsWith("Log out"))).perform(click());
    }

    @Test
    public static void loginWithInvalidCredentials() {
        clickOn(R.id.button_login);
        clickOn(R.id.button_email);
        writeTo(R.id.input_email, "test.espresso.00005@gmail.com");
        writeTo(R.id.input_password, "*-Re7]J4Ux8q)g?X");
        clickOn(R.id.button);
        assertContains(R.string.simperium_dialog_message_login);
    }


    public static void swipe(int start, int end, int delay) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        Instrumentation inst = getInstrumentation();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 500, start, 0);
        inst.sendPointerSync(event);
        eventTime = SystemClock.uptimeMillis() + delay;
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, 500, end, 0);
        inst.sendPointerSync(event);
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 500, end, 0);
        inst.sendPointerSync(event);
        SystemClock.sleep(2000);
    }

    // Swipe all the way to the bottom of the screen
    public static void swipeToBottom() {
        swipe(1000, 100, 0);
    }

    // Swipe down one page at a time
    public static void scrollSlowlyDown() {
        swipe(775, 100, 100);
    }

    // Swipe to the top
    public static void swipeToTop() {
        swipe(100, 1000, 0);
    }

    // Swipe up one page at a time
    public static void scrollSlowlyUp() {
        swipe(100, 775, 100);
    }

}