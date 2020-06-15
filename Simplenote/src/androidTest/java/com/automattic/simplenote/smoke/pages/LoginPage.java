package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.action.ViewActions.click;

public class LoginPage extends BasePage {

    public void login(String email, String password) {

        enterTextInCustomInput(R.id.input_email, email);
        enterTextInCustomInput(R.id.input_password, password);

        getViewById(R.id.button).perform(click());

        TestUtils.giveMeABreak();
    }

    public void checkLoginFailedMessage() {
        waitForElementToBeDisplayed(R.id.alertTitle);
    }

    public Boolean isLoginFailed() {
        return isElementDisplayed(R.id.alertTitle);
    }
}
