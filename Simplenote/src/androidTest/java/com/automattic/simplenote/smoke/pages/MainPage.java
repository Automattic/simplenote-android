package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.data.NoteDTO;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class MainPage extends BasePage {

    private static final Integer MENU_SEARCH = R.id.menu_search;
    private static final Integer BUTTON_FAB = R.id.fab_button;
    private static final Integer TEXT_NOTE_TITLE = R.id.note_title;

    private NavigationMenu navigationMenu;
    private OptionsMenu optionsMenu;

    public MainPage() {
        navigationMenu = new NavigationMenu();
    }

    public MainPage isOpened() {
        waitForElementToBeDisplayed(MENU_SEARCH);
        return this;
    }

    public IntroPage logout() {
        navigationMenu.openSettings().logout();

        return new IntroPage();
    }

    public MainPage addNewNote(NoteDTO noteDTO) {
        waitForElementToBeDisplayed(BUTTON_FAB);

        clickButton(BUTTON_FAB);
        new NotePage()
                .enterNewNote(noteDTO)
                .pressBack();

        waitForElementToBeDisplayed(MENU_SEARCH);

        return this;
    }

    public MainPage checkNoteIsInTheList(NoteDTO noteDTO) {
        checkElementDisplayedWithTextAtPosition(TEXT_NOTE_TITLE, noteDTO.getTitle(), 0);

        return this;
    }

    public MainPage checkNoteIsNotInTheList(NoteDTO noteDTO) {
        onView(allOf(withId(TEXT_NOTE_TITLE), withText(noteDTO.getTitle()))).check(doesNotExist());

        return this;
    }

    public NotePage openNote(NoteDTO noteDTO) {
        clickElementDisplayedWithTextAtPosition(TEXT_NOTE_TITLE, noteDTO.getTitle(), 0);
        TestUtils.giveMeABreak();

        return new NotePage();
    }

    public SearchPage openSearchPage() {
        clickButton(MENU_SEARCH);

        return new SearchPage();
    }

    public SearchPage selectTagFromDrawer(String tag) {
        navigationMenu.selectTag(tag);

        return new SearchPage();
    }

    public TrashPage openTrashPage() {

        return navigationMenu.openTrash();
    }

}
