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
        NEWEST_MODIFIED_DATE(R.string.sort_newest_modified),
        OLDEST_MODIFIED_DATE(R.string.sort_oldest_modified),
        NEWEST_CREATED_DATE(R.string.sort_newest_created),
        OLDEST_CREATED_DATE(R.string.sort_oldest_created),
        ALPHABETICALLY_A_Z(R.string.sort_alphabetical),
        ALPHABETICALLY_Z_A(R.string.sort_alphabetical_reverse);

        private Integer itemText;

        public Integer getItemText() {
            return itemText;
        }

        SortOrder(Integer itemText) {
            this.itemText = itemText;
        }
    }

    private static final Integer BUTTON_SORT_ORDER = android.R.id.text1;
    private static final Integer TEXT_SHARE_ANALYTICS = R.string.share_analytics;
    private static final Integer TEXT_CONDENSED_NOTES = R.string.condensed_note_list;
    private static final Integer MENU_SORT_ORDER = R.string.sort_order;
    private static final Integer SWITCH_WIDGET = R.id.switchWidget;
    private static final Integer BUTTON_DELETE_NOTES = android.R.id.button1;
    private static final Integer RECYCLER_VIEW = R.id.recycler_view;

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
        onView(allOf(isDisplayed(), withId(RECYCLER_VIEW)))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(MENU_SORT_ORDER)),
                        click()));

        TestUtils.idleForAShortPeriod();
    }

    private void switchComponentIfApplicable(boolean state, Integer itemFinderTextResourceId) {
        scrollOnRecyclerViewForText(itemFinderTextResourceId);

        try {
            if (state) {
                getSwitchComponent(itemFinderTextResourceId).check(matches(isNotChecked())).perform(click());
            } else {
                getSwitchComponent(itemFinderTextResourceId).check(matches(isChecked())).perform(click());
            }
        } catch (AssertionError e) {
            System.err.println("Given state and the switch components state are same!");
        }

    }

    private void scrollOnRecyclerViewForText(Integer searchParamResourceId) {
        onView(allOf(isDisplayed(), withId(RECYCLER_VIEW)))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(searchParamResourceId)),
                        scrollTo()));
    }

    private ViewInteraction getSwitchComponent(Integer itemText) {
        return onView(
                allOf(withId(SWITCH_WIDGET), withParent(withParent(hasDescendant(withText(itemText)))))
        );
    }
}
