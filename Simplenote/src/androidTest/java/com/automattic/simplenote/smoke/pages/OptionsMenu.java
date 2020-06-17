package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.ViewInteraction;

import com.automattic.simplenote.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

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

    public void pin() {
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
        ViewInteraction materialTextView = onView(
                allOf(withId(R.id.title), withText(optionsMenuItem.getMenuText()),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()))
                .perform(click());
    }

    private void open() {
        onView(
                allOf(withContentDescription(MENU_DESCRIPTION),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        1),
                                POSITION_MENU),
                        isDisplayed()))
                .perform(click());
    }
}
