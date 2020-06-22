package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
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

    private void open() {
        onView(withContentDescription(MENU_DESCRIPTION)).perform(click());
    }
}
