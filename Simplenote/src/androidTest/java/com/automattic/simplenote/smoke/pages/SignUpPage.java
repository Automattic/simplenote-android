package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.action.ViewActions.click;

public class SignUpPage extends BasePage {

    private static final Integer INPUT_EMAIL = R.id.input_email;
    private static final Integer INPUT_PASSWORD = R.id.input_password;
    private static final Integer BUTTON_SIGNUP = R.id.button;


    public MainPage signUp(String email, String password) {

        enterTextInCustomInput(INPUT_EMAIL, email);
        enterTextInCustomInput(INPUT_PASSWORD, password);

        getViewById(BUTTON_SIGNUP).perform(click());

        TestUtils.idleForAShortPeriod();

        return new MainPage();
    }
}
