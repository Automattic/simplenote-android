package com.automattic.simplenote.smoke.pages;

import androidx.test.espresso.action.ViewActions;

import com.automattic.simplenote.R;

import static androidx.test.espresso.action.ViewActions.click;

public class LoginPage extends BasePage {

    public MainPage login(String email, String password) {
        getViewById(R.id.input_email)
                .perform(click())
                .perform(replaceTextInCustomInput(email))
                .perform(ViewActions.closeSoftKeyboard());

        getViewById(R.id.input_password)
                .perform(click())
                .perform(replaceTextInCustomInput(password))
                .perform(ViewActions.closeSoftKeyboard());

        getViewById(R.id.button).perform(click());

        return new MainPage();
    }
}
