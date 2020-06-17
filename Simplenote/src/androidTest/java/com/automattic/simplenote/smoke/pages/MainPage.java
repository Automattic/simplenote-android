package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.data.NoteDTO;

public class MainPage extends BasePage {

    private static final Integer MENU_SEARCH = R.id.menu_search;
    private static final Integer BUTTON_FAB = R.id.fab_button;

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

    public void addNewNote(NoteDTO noteDTO) {
        clickButton(BUTTON_FAB);
        new NotePage()
                .enterNewNote(noteDTO)
                .pressBack();
        
        waitForElementToBeDisplayed(R.id.menu_search);
    }

    public SearchPage openSearchPage() {
        clickButton(MENU_SEARCH);

        return new SearchPage();
    }
}
