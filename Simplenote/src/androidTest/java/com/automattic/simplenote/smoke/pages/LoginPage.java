package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.action.ViewActions.click;

public class LoginPage extends BasePage {

    private static final Integer INPUT_EMAIL = R.id.input_email;
    private static final Integer INPUT_PASSWORD = R.id.input_password;
    private static final Integer BUTTON_LOGIN = R.id.button;
    private static final Integer ALERT_LOGIN = R.id.alertTitle;


    public void login(String email, String password) {

        enterTextInCustomInput(INPUT_EMAIL, email);
        enterTextInCustomInput(INPUT_PASSWORD, password);

        getViewById(BUTTON_LOGIN).perform(click());

        TestUtils.idleForAShortPeriod();
    }

    public void checkLoginFailedMessage() {
        waitForElementToBeDisplayed(ALERT_LOGIN);
    }

    public Boolean isLoginFailed() {
        return isElementDisplayed(ALERT_LOGIN);
    }
}
