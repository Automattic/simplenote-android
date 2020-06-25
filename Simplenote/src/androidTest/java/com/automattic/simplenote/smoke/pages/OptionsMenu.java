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

public class OptionsMenu extends BasePage {

    private static final Integer BUTTON_MORE_OPTIONS = R.string.more_options;
    private static final Integer ITEM_TITLE = R.id.title;
    private static final Integer CHECKBOX = R.id.checkbox;

    enum OptionsMenuItem {
        PIN(R.string.pin),
        MARKDOWN(R.string.markdown),
        SHARE(R.string.share),
        HISTORY(R.string.history),
        TRASH(R.string.trash),
        RESTORE(R.string.restore),
        PUBLISH(R.string.publish),
        COPY_LINK(R.string.copy_link);

        private Integer menuText;

        public Integer getMenuText() {
            return menuText;
        }

        OptionsMenuItem(Integer menuText) {
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
        onView(allOf(withId(ITEM_TITLE), withText(optionsMenuItem.getMenuText()))).perform(click());
    }

    private ViewInteraction getCheckBoxComponent(OptionsMenuItem optionsMenuItem) {
        return onView(
                allOf(withId(CHECKBOX), withParent(hasDescendant(withText(optionsMenuItem.getMenuText()))))
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
        if (isElementDisplayed(onView(withContentDescription(BUTTON_MORE_OPTIONS)))) {
            onView(withContentDescription(BUTTON_MORE_OPTIONS)).perform(click());
        }
    }
}
