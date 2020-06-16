package com.automattic.simplenote.smoke.pages;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;

import com.automattic.simplenote.smoke.support.SupplierIdler;
import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

public class BasePage {
    protected void clickButton(Integer resourceId) {
        onView(allOf(withId(resourceId), isDisplayed())).perform(click());
    }

    protected void enterTextInCustomInput(Integer resourceId, String text) {
        getViewById(resourceId)
                .perform(click())
                .perform(replaceTextInCustomInput(text))
                .perform(ViewActions.closeSoftKeyboard());
    }

    protected void enterText(Integer resourceId, String text) {
        getViewById(resourceId)
                .perform(click())
                .perform(replaceText(text))
                .perform(ViewActions.closeSoftKeyboard());
    }

    public Boolean isViewDisplayed(ViewInteraction view) {
        try {
            view.check(matches(isDisplayed()));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public ViewInteraction getViewById(Integer id) {
        return onView(allOf(withId(id), isDisplayed()));
    }

    public void pressBack() {
        Espresso.pressBack();
    }

    // Thanks to https://stackoverflow.com/a/47412904/809944
    private static ViewAction replaceTextInCustomInput(final String text) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextInputLayout.class));
            }

            @Override
            public String getDescription() {
                return "Replace text in TextInputLayout view";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextInputLayout) view).getEditText().setText(text);
            }
        };
    }

    protected void waitForViewMatching(final Matcher<View> matcher, final long milliseconds) {
        onView(isRoot()).perform(waitForViewToBeDisplayed(matcher, milliseconds));
    }

    public static ViewAction waitForViewToBeDisplayed(final Matcher<View> matcher, final long milliseconds) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Wait for a view matching the given matcher for " + milliseconds + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + milliseconds;

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found matching view
                        if (matcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    // WAITERS
    public static void waitForElementToBeDisplayed(final Integer elementID) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return isElementDisplayed(elementID);
            }
        });
    }

    public static void waitForConditionToBeTrue(Supplier<Boolean> supplier) {
        if (supplier.get()) {
            return;
        }

        new SupplierIdler(supplier).idleUntilReady();
    }

    public static boolean isElementDisplayed(Integer elementID) {
        return isElementDisplayed(visibleElementWithId(elementID));
    }

    public static boolean isElementDisplayed(ViewInteraction element) {
        try {
            element.check(matches(isDisplayed()));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }


    public static ViewInteraction visibleElementWithId(Integer elementID) {
        return onView(allOf(withId(elementID), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    protected static Matcher<View> childAtPosition(
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
