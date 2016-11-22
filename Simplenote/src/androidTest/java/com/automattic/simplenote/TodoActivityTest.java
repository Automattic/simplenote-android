package com.automattic.simplenote;


import android.os.SystemClock;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TodoActivityTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void todoActivityTest() {
        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.fab_button),
                        withParent(allOf(withId(R.id.list_root),
                                withParent(withId(R.id.note_fragment_container)))),
                        isDisplayed()));
        SystemClock.sleep(100);

        floatingActionButton.perform(click());
        SystemClock.sleep(100);

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        SystemClock.sleep(100);

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.title), withText("Преобразовать в Todo"), isDisplayed()));
        appCompatTextView.perform(click());
        SystemClock.sleep(100);

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.todo_add_text),
                        withParent(allOf(withId(R.id.todo_add),
                                withParent(withId(R.id.todo_component)))),
                        isDisplayed()));
        SystemClock.sleep(100);

        appCompatEditText.perform(replaceText("todo\n"), closeSoftKeyboard());
        SystemClock.sleep(100);

                ViewInteraction editText = onView(
                        allOf(withId(R.id.todo_title), withText("todo"),
                                childAtPosition(
                                        childAtPosition(
                                                withId(R.id.todo_row),
                                                0),
                                        2),
                                isDisplayed()));
        SystemClock.sleep(100);
        
        ViewInteraction appCompatCheckBox = onView(
                allOf(withId(R.id.todo_checked), isDisplayed()));
        SystemClock.sleep(100);

        appCompatCheckBox.perform(click());
        SystemClock.sleep(100);

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(R.id.todo_remove), withText("✖"),
                        withParent(allOf(withId(R.id.todo_row),
                                withParent(withClassName(is("com.mobeta.android.dslv.DragSortItemView"))))),
                        isDisplayed()));
        SystemClock.sleep(100);
        appCompatTextView2.perform(click());
        SystemClock.sleep(100);

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
