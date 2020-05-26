package com.automattic.simplenote;


import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringEndsWith.endsWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class LoginWithCorrectPasswordLogoutTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void loginWithCorrectPasswordLogoutTest() {

        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //login with email and logout

        ViewInteraction loginButton = onView(
                allOf(withId(R.id.button_login), withText("Log In"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                4),
                        isDisplayed()));
        loginButton.perform(click());

        ViewInteraction loginWithEmailButton = onView(
                allOf(withId(R.id.button_email), withText("Log In with Email"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.design_bottom_sheet),
                                        0),
                                0),
                        isDisplayed()));
        loginWithEmailButton.perform(click());

        ViewInteraction emailField = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.input_email),
                                0),
                        0),
                        isDisplayed()));
        emailField.perform(replaceText("test.espresso.00001@gmail.com"), closeSoftKeyboard());

        ViewInteraction pwField = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.input_password),
                                0),
                        0),
                        isDisplayed()));
        pwField.perform(replaceText("*-Re7]J4Ux8q)g?X"), closeSoftKeyboard());

        ViewInteraction loginButton2 = onView(
                allOf(withId(R.id.button), withText("Log In"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.main),
                                        1),
                                2),
                        isDisplayed()));
        loginButton2.perform(click());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction openDrawer = onView(
                allOf(withContentDescription("Open drawer"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withClassName(is("com.google.android.material.appbar.AppBarLayout")),
                                                0)),
                                1),
                        isDisplayed()));
        openDrawer.perform(click());

        ViewInteraction navigationMenuItemView = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.navigation_view),
                                        0)),
                        3),
                        isDisplayed()));
        navigationMenuItemView.perform(click());

        swipeToBottom();

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withText(endsWith("Log out"))).perform(click());

    }


    private static void swipe(int start, int end, int delay) {
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

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
