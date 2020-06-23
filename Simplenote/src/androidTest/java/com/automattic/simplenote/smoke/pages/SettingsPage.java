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

    public enum SortOrder {
        NEWEST_MODIFIED_DATE("Newest modified date"),
        OLDEST_MODIFIED_DATE("Oldest modified date"),
        NEWEST_CREATED_DATE("Newest created date"),
        OLDEST_CREATED_DATE("Oldest created date"),
        ALPHABETICALLY_A_Z("Alphabetically, A-Z"),
        ALPHABETICALLY_Z_A("Alphabetically, Z-A");

        private String itemText;

        public String getItemText() {
            return itemText;
        }

        SortOrder(String itemText) {
            this.itemText = itemText;
        }
    }


    private static final Integer BUTTON_SORT_ORDER = android.R.id.text1;
    private static final String TEXT_SHARE_ANALYTICS = "Share analytics";
    private static final String TEXT_CONDENSED_NOTES = "Condensed note list";
    private static final String MENU_SORT_ORDER = "Sort order";
    private static final Integer SWITCH_WIDGET = R.id.switchWidget;
    private static final Integer BUTTON_DELETE_NOTES = android.R.id.button1;

    public void logout(String loginEmail) {

        onView(allOf(isDisplayed(), withId(R.id.recycler_view)))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(containsString(loginEmail))),
                        click()));

        if (isElementDisplayed(BUTTON_DELETE_NOTES)) {
            clickButton(BUTTON_DELETE_NOTES);
        }

        TestUtils.idleForAShortPeriod();
    }


    public SettingsPage switchShareAnalytics(boolean state) {
        switchComponentIfApplicable(state, TEXT_SHARE_ANALYTICS);

        return this;
    }

    public SettingsPage switchCondensedMode(boolean state) {
        switchComponentIfApplicable(state, TEXT_CONDENSED_NOTES);

        return this;
    }

    public SettingsPage changeOrder(SortOrder newOrder) {
        openSortOrderDialog();

        clickButton(BUTTON_SORT_ORDER, newOrder.getItemText());
        TestUtils.idleForAShortPeriod();
        return this;
    }

    private void openSortOrderDialog() {
        onView(allOf(isDisplayed(), withId(R.id.recycler_view)))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(MENU_SORT_ORDER)),
                        click()));

        TestUtils.idleForAShortPeriod();
    }

    private void switchComponentIfApplicable(boolean state, String itemFinderText) {
        scrollOnRecyclerViewForText(itemFinderText);

        try {
            if (state) {
                getSwitchComponent(itemFinderText).check(matches(isNotChecked())).perform(click());
            } else {
                getSwitchComponent(itemFinderText).check(matches(isChecked())).perform(click());
            }
        } catch (AssertionError e) {
            System.err.println("Given state and the switch components state are same!");
        }

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
