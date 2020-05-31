package com.automattic.simplenote.espressoUITestsHelpers;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.rule.ActivityTestRule;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;

public class EspressoGeneralHelpers {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public static void tapNoteButton() {
        clickOn(R.id.fab_button);
    }

    @Test
    public static void tapOutside() {
        clickOn(R.id.touch_outside);
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
    public static void swipeSlowlyDown() {
        swipe(775, 100, 100);
    }

    // Swipe to the top
    public static void swipeToTop() {
        swipe(100, 1000, 0);
    }

    // Swipe up one page at a time
    public static void swipeSlowlyUp() {
        swipe(100, 775, 100);
    }

}