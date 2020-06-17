package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.contrib.RecyclerViewActions;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

public class SettingsPage extends BasePage {

    private static final Integer TEXT_LOGOUT = R.string.log_out;

    private static final Integer POSITION_LOGOUT = 14;

    public void logout() {
        selectSettingsOption(TEXT_LOGOUT, POSITION_LOGOUT);

        TestUtils.giveMeABreak();
    }

    private void selectSettingsOption(Integer textId, Integer position) {

        onView(allOf(isDisplayed(), withId(R.id.recycler_view)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(POSITION_LOGOUT, click()));
    }
}
