package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

public class SettingsPage extends BasePage {

    private static final String TEXT_SHARE_ANALYTICS = "Share analytics";
    private static final Integer SWITCH_WIDGET = R.id.switchWidget;

    public void logout(String emailAddress) {
        onView(allOf(isDisplayed(), withId(R.id.recycler_view)))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(emailAddress)),
                        click()));

        TestUtils.idleForAShortPeriod();
    }

    public SettingsPage shareAnalytics(boolean state) {

        scrollOnRecyclerViewForText(TEXT_SHARE_ANALYTICS);

        try {
            if (state) {
                getSwitchComponent(TEXT_SHARE_ANALYTICS).check(matches(isNotChecked())).perform(click());
            } else {
                getSwitchComponent(TEXT_SHARE_ANALYTICS).check(matches(isChecked())).perform(click());
            }
        } catch (AssertionError e) {
            System.err.println("Given state and the switch components state are same!");
        }

        return this;
    }

    private void scrollOnRecyclerViewForText(String searchParam) {
        onView(allOf(isDisplayed(), withId(R.id.recycler_view)))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(containsString(searchParam))),
                        scrollTo()));
    }

    private ViewInteraction getSwitchComponent(String itemText) {
        return onView(
                allOf(withId(SWITCH_WIDGET), withParent(withParent(hasDescendant(withText(containsString(itemText))))))
        );
    }
}
