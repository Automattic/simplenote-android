package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.contrib.RecyclerViewActions;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class SettingsPage extends BasePage {

    public void logout(String emailAddress) {
        onView(allOf(isDisplayed(), withId(R.id.recycler_view)))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(emailAddress)),
                        click()));

        TestUtils.idleForAShortPeriod();
    }
}
