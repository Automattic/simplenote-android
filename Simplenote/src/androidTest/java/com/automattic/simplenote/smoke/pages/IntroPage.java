package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;

public class IntroPage extends BasePage {

    private static final Integer BUTTON_LOGIN = R.id.button_login;
    private static final Integer BUTTON_EMAIL = R.id.button_email;
    private static final Integer BUTTON_OTHER = R.id.button_other;


    public LoginPage goToLoginWithEmail() {

        waitForElementToBeDisplayed(BUTTON_LOGIN);
        clickButton(BUTTON_LOGIN);

        waitForElementToBeDisplayed(BUTTON_EMAIL);
        clickButton(BUTTON_EMAIL);

        return new LoginPage();
    }

    public Boolean isOpened() {
        return isElementDisplayed(BUTTON_LOGIN);
    }

    /**
     * Call the login with other page (current usage is "Log in with WordPress.com")
     * TODO currently forwarded to standard "Log in with email" page
     *
     * @return fresh Login page
     */
    public LoginPage goToLoginWithWordpress() {
        clickButton(BUTTON_LOGIN);
        clickButton(BUTTON_OTHER);

        return new LoginPage();
    }
}
