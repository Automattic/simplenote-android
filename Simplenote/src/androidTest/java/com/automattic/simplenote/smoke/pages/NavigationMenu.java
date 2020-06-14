package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.contrib.DrawerActions;

import com.automattic.simplenote.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

public class NavigationMenu extends BasePage {

    private void openMenu() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    }

    public SettingsPage openSettings() {

        openMenu();

        onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.navigation_view),
                                        0)),
                        3),
                        isDisplayed())).perform(click());

        return new SettingsPage();
    }
}
