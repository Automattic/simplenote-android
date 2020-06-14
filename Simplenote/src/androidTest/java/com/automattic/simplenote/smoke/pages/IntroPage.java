package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;

public class IntroPage extends BasePage {

    public LoginPage goToLoginWithEmail() {
        clickButton(R.id.button_login);
        clickButton(R.id.button_email);

        return new LoginPage();
    }

    /**
     * Call the login with other page (current usage is "Log in with WordPress.com")
     * TODO currently forwarded to standart "Log in with email" page
     *
     * @return fresh Login page
     */
    public LoginPage goToLoginWithWordpress() {
        clickButton(R.id.button_login);
        clickButton(R.id.button_other);

        return new LoginPage();
    }
}
