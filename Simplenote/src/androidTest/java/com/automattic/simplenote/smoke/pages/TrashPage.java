package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

public class TrashPage extends BasePage {

    private static final Integer BUTTON_EMPTY_TRASH = R.id.menu_empty_trash;
    private static final Integer BUTTON_YES = R.id.button1;
    private static final Integer TEXT_NOTE_TITLE = R.id.note_title;
    private NavigationMenu navigationMenu;

    public TrashPage() {
        navigationMenu = new NavigationMenu();
    }

    public TrashPage emptyTrash() {
        clickButton(BUTTON_EMPTY_TRASH);
        if (isElementDisplayed(BUTTON_YES)) {
            clickButton(BUTTON_YES);
        }

        TestUtils.idleForAShortPeriod();

        return this;
    }

    public MainPage openMain() {
        return navigationMenu.openMain();
    }

    public TrashPage checkNoteIsTrashed(String searchParam) {
        checkElementDisplayedWithTextAtPosition(TEXT_NOTE_TITLE, searchParam, 0);

        return this;
    }

    public NotePage openTrashedNote(String searchParam) {
        TestUtils.idleForAShortPeriod();

        clickElementDisplayedWithTextAtPosition(R.id.note_title, searchParam, 0);

        return new NotePage();
    }
}
