package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;

public class MainPage extends BasePage {

    public NavigationMenu navigationMenu;

    public Boolean isOpen() {
        return isViewDisplayed(getViewById(R.id.list_root));
    }

    public MainPage() {
        navigationMenu = new NavigationMenu();
    }

    public LoginPage logout() {
        navigationMenu.openSettings().logout();

        return new LoginPage();
    }
}
