package com.automattic.simplenote;


import android.os.SystemClock;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ColorActivityTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void colorActivityTest() {
        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.fab_button),
                        withParent(allOf(withId(R.id.list_root),
                                withParent(withId(R.id.note_fragment_container)))),
                        isDisplayed()));
        floatingActionButton.perform(click());
        SystemClock.sleep(500);
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        SystemClock.sleep(500);

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.title), withText("Цвет заметки"), isDisplayed()));
        appCompatTextView.perform(click());
        SystemClock.sleep(500);

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(56789999), isDisplayed()));

        appCompatTextView2.perform(click());

        SystemClock.sleep(500);

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.reset_color), withText("Сбросить цвет"), isDisplayed()));
        appCompatButton.perform(click());
        SystemClock.sleep(500);

        ViewInteraction view = onView(
                allOf(withId(R.id.touch_outside), isDisplayed()));
        view.perform(click());

    }

}
