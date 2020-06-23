package com.automattic.simplenote.smoke.test;

import androidx.test.rule.ActivityTestRule;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.smoke.data.DataProvider;
import com.automattic.simplenote.smoke.data.NoteDTO;
import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;
import com.automattic.simplenote.smoke.pages.SettingsPage.SortOrder;
import com.automattic.simplenote.smoke.utils.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRunner {
    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);


    @Before
    public void init() {
        TestUtils.idleForAShortPeriod();
        TestUtils.logoutIfNecessary();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLogin() {

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .isOpened()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testLogout() {

        if (!TestUtils.logoutIfNecessary()) {
            new IntroPage()
                    .goToLoginWithEmail()
                    .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

            new MainPage()
                    .isOpened()
                    .logout(DataProvider.LOGIN_EMAIL);
        }
    }

    @Test
    public void testLoginWithWrongPassword() {
        TestUtils.logoutIfNecessary();

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_WRONG_PASSWORD);

        new LoginPage()
                .checkLoginFailedMessage();
    }

    @Test
    public void testSearchingInTheSearchFieldDoesAGlobalSearch() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        String searchParam = noteDTO.getTitle();

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .switchCondensedNoteListMode(false)
                .pressBack();

        new MainPage()
                .addNewNote(noteDTO)
                .openSearchPage()
                .search(searchParam)
                .checkSearchResultsTitleAndContent(searchParam);

        new MainPage()
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test //
    public void testFilterByTagWhenClickingOnTagInTagDrawer() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .switchCondensedNoteListMode(false)
                .pressBack();

        new MainPage()
                .addNewNote(noteDTO)
                .selectTagFromDrawer(noteDTO.getTags().get(0))
                .checkNoteContentIsInTheList(noteDTO.getContent().substring(0, 15));

        new MainPage()
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test//
    public void testViewTrashedNotesByClickingOnTrash() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .trash()
                .openTrashPage()
                .checkNoteIsTrashed(noteDTO.getTitle());

        new MainPage()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test//
    public void testRestoreNoteFromTrashScreen() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .trash()
                .openTrashPage()
                .openTrashedNote(noteDTO.getTitle())
                .restore()
                .openMain();

        new MainPage()
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testTrashNote() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .trash()
                .checkNoteTitleIsNotInTheList(noteDTO);

        new MainPage()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testShareAnalytics() {

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .switchShareAnalytics(true)
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testCondensedModeOpened() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .switchCondensedNoteListMode(false)
                .pressBack();

        new MainPage()
                .checkNoteContentIsInTheList(noteDTO.getContent().substring(0, 15))
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testCondensedModeClosed() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .switchCondensedNoteListMode(true)
                .pressBack();

        new MainPage()
                .checkNoteContentIsNotInTheList(noteDTO.getContent().substring(0, 15))
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderAlphabeticallyAZ() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .switchPinMode()
                .pressBack();

        new MainPage()
                .changeOrder(SortOrder.ALPHABETICALLY_A_Z)
                .pressBack();

        new MainPage()
                .checkNoteInTheGivenPosition(noteDTO.getTitle(), 0)
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderAlphabeticallyZA() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .switchPinMode()
                .pressBack();

        new MainPage()
                .changeOrder(SortOrder.ALPHABETICALLY_Z_A)
                .pressBack();

        new MainPage()
                .checkNoteInTheGivenPosition(noteDTO.getTitle(), 0)
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);

    }

    @Test
    public void testPinnedNotesWhileChangingOrderOldestModifiedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .switchPinMode()
                .pressBack();

        new MainPage()
                .changeOrder(SortOrder.OLDEST_MODIFIED_DATE)
                .pressBack();

        new MainPage()
                .checkNoteInTheGivenPosition(noteDTO.getTitle(), 0)
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderNewestModifiedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .switchPinMode()
                .pressBack();

        new MainPage()
                .changeOrder(SortOrder.NEWEST_MODIFIED_DATE)
                .pressBack();

        new MainPage()
                .checkNoteInTheGivenPosition(noteDTO.getTitle(), 0)
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderOldestCreatedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .switchPinMode()
                .pressBack();

        new MainPage()
                .changeOrder(SortOrder.OLDEST_CREATED_DATE)
                .pressBack();

        new MainPage()
                .checkNoteInTheGivenPosition(noteDTO.getTitle(), 0)
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderNewestCreatedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .switchPinMode()
                .pressBack();

        new MainPage()
                .changeOrder(SortOrder.NEWEST_CREATED_DATE)
                .pressBack();

        new MainPage()
                .checkNoteInTheGivenPosition(noteDTO.getTitle(), 0)
                .openNote(noteDTO)
                .trash()
                .logout(DataProvider.LOGIN_EMAIL);
    }
}
