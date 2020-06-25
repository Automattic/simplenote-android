package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.contrib.DrawerActions;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class NavigationMenu extends BasePage {

    private static final Integer BUTTON_TAG = R.id.design_menu_item_text;
    private static final Integer LAYOUT_DRAWER = R.id.drawer_layout;


    enum NavigationMenuItem {
        SETTINGS(R.string.settings),
        MAIN(R.string.all_notes),
        TRASH(R.string.trash);

        public Integer getTitle() {
            return title;
        }

        private Integer title;

        NavigationMenuItem(Integer title) {
            this.title = title;
        }
    }

    private void openMenu() {
        waitForElementToBeDisplayed(LAYOUT_DRAWER);
        onView(withId(LAYOUT_DRAWER)).perform(DrawerActions.open());
    }

    public MainPage openMain() {
        openMenu();

        clickDrawerStaticItem(NavigationMenuItem.MAIN);

        return new MainPage();
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

        clickButton(BUTTON_TAG, navigationMenuItem.getTitle());

        TestUtils.idleForAShortPeriod();
    }

    public void selectTag(String tag) {
        openMenu();

        clickButton(BUTTON_TAG, tag);

        TestUtils.idleForAShortPeriod();
    }
}
