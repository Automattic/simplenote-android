package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.ViewInteraction;

import com.automattic.simplenote.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

public class OptionsMenu extends BasePage {

    private static final Integer POSITION_MENU = 2;
    private static final String MENU_DESCRIPTION = "More Options";

    enum OptionsMenuItem {
        PIN("Pin"),
        MARKDOWN("Markdown"),
        SHARE("Share"),
        HISTORY("History"),
        TRASH("Trash"),
        RESTORE("Restore"),
        PUBLISH("Publish"),
        COPY_LINK("Copy Link");

        private String menuText;

        public String getMenuText() {
            return menuText;
        }

        OptionsMenuItem(String menuText) {
            this.menuText = menuText;
        }
    }

    public void markdown(boolean state) {
        open();

        switchComponentIfApplicable(state, OptionsMenuItem.MARKDOWN);
    }

    public void switchPinMode() {
        open();

        clickStaticItem(OptionsMenuItem.PIN);
    }

    public void trash() {
        open();

        clickStaticItem(OptionsMenuItem.TRASH);
    }

    public void restore() {
        open();

        clickStaticItem(OptionsMenuItem.RESTORE);
    }

    /**
     * Used for clicking static items of the options menu for given position
     */
    private void clickStaticItem(OptionsMenuItem optionsMenuItem) {
        onView(allOf(withId(R.id.title), withText(optionsMenuItem.getMenuText()))).perform(click());
    }

    private ViewInteraction getCheckBoxComponent(OptionsMenuItem optionsMenuItem) {
        return onView(
                allOf(withId(R.id.checkbox), withParent(hasDescendant(withText(containsString(optionsMenuItem.getMenuText())))))
        );
    }

    private void switchComponentIfApplicable(boolean state, OptionsMenuItem optionsMenuItem) {

        try {
            if (state) {
                getCheckBoxComponent(optionsMenuItem).check(matches(isNotChecked())).perform(click());
            } else {
                getCheckBoxComponent(optionsMenuItem).check(matches(isChecked())).perform(click());
            }
        } catch (AssertionError e) {
            System.err.println("Given state and the switch components state are same!");
            // Close options menu without clicking any item
            pressBack();
        }

    }

    private void open() {
        onView(withContentDescription(MENU_DESCRIPTION)).perform(click());
    }
}
