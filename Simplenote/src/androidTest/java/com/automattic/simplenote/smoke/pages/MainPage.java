package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;

public class MainPage extends BasePage {

    public NavigationMenu navigationMenu;

    public MainPage() {
        navigationMenu = new NavigationMenu();
    }

    public MainPage isOpened() {
        waitForElementToBeDisplayed(R.id.menu_search);

        return this;
    }

    public IntroPage logout() {
        navigationMenu.openSettings().logout();

        return new IntroPage();
    }
}
