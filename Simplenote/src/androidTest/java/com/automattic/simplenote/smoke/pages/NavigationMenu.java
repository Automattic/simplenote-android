package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.contrib.DrawerActions;

import com.automattic.simplenote.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class NavigationMenu extends BasePage {

    private static final Integer BUTTON_TAG = R.id.design_menu_item_text;
    private static final Integer LAYOUT_DRAWER = R.id.drawer_layout;
    private static final Integer LAYOUT_DESIGN_NAVIGATION_VIEW = R.id.design_navigation_view;
    private static final Integer LAYOUT_NAVIGATION_VIEW = R.id.navigation_view;


    enum NavigationMenuItem {
        SETTINGS(3),
        TRASH(2);

        public int getPosition() {
            return position;
        }

        private int position;

        NavigationMenuItem(int position) {
            this.position = position;
        }
    }

    private void openMenu() {
        waitForElementToBeDisplayed(LAYOUT_DRAWER);
        onView(withId(LAYOUT_DRAWER)).perform(DrawerActions.open());
    }

    public SettingsPage openSettings() {
        openMenu();

        clickDrawerStaticItem(NavigationMenuItem.SETTINGS);

        return new SettingsPage();
    }

    public TrashPage openTrash() {
        openMenu();

        clickDrawerStaticItem(NavigationMenuItem.TRASH);

        return new TrashPage();
    }

    /**
     * Used for clicking static items of the drawer menu for given position
     */
    private void clickDrawerStaticItem(NavigationMenuItem navigationMenuItem) {
        onView(
                allOf(childAtPosition(
                        allOf(withId(LAYOUT_DESIGN_NAVIGATION_VIEW),
                                childAtPosition(
                                        withId(LAYOUT_NAVIGATION_VIEW),
                                        0)),
                        navigationMenuItem.getPosition()),
                        isDisplayed())).perform(click());

    }

    public void selectTag(String tag) {
        openMenu();

        onView(allOf(withId(BUTTON_TAG), withText(tag))).perform(click());
    }
}
