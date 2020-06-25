package com.automattic.simplenote.smoke.test;

import androidx.test.rule.ActivityTestRule;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.smoke.data.DataProvider;
import com.automattic.simplenote.smoke.data.NoteDTO;
import com.automattic.simplenote.smoke.data.SignUpDataProvider;
import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;
import com.automattic.simplenote.smoke.pages.NotePage;
import com.automattic.simplenote.smoke.pages.SettingsPage.SortOrder;
import com.automattic.simplenote.smoke.utils.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRunner {
    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class, true, true);

    private static String email;
    private static String password;

    @Before
    public void init() {
        TestUtils.logoutIfNecessary();

        if (email == null || password == null) {

            email = SignUpDataProvider.generateEmail();
            password = SignUpDataProvider.generatePassword();

            System.out.println("Email: " + email);
            System.out.println("Password: " + password);

            new IntroPage()
                    .openSignUp()
                    .signUp(email, password)
                    .logout(email);
        }
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLogin() {

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .isOpened()
                .logout(email);
    }

    @Test
    public void testLogout() {

        if (!TestUtils.logoutIfNecessary()) {
            new IntroPage()
                    .goToLoginWithEmail()
                    .login(email, password);

            new MainPage()
                    .isOpened()
                    .logout(email);
        }
    }

    @Test
    public void testLoginWithWrongPassword() {
        new IntroPage()
                .goToLoginWithEmail()
                .login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        new LoginPage()
                .checkLoginFailedMessage();
    }

    @Test
    public void testSearchingInTheSearchFieldDoesAGlobalSearch() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        String searchParam = noteDTO.getTitle();

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testFilterByTagWhenClickingOnTagInTagDrawer() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testViewTrashedNotesByClickingOnTrash() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .trash()
                .openTrashPage()
                .checkNoteIsTrashed(noteDTO.getTitle());

        new MainPage()
                .logout(email);
    }

    @Test
    public void testRestoreNoteFromTrashScreen() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testTrashNote() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .trash()
                .checkNoteTitleIsNotInTheList(noteDTO);

        new MainPage()
                .logout(email);
    }

    @Test
    public void testShareAnalytics() {

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .switchShareAnalytics(true)
                .logout(email);
    }

    @Test
    public void testCondensedModeClosed() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .switchCondensedNoteListMode(false)
                .pressBack();

        new MainPage()
                .checkNoteContentIsInTheList(noteDTO.getContent().substring(0, 15))
                .openNote(noteDTO)
                .trash()
                .logout(email);
    }

    @Test
    public void testCondensedModeOpened() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .switchCondensedNoteListMode(true)
                .pressBack();

        new MainPage()
                .checkNoteContentIsNotInTheList(noteDTO.getContent().substring(0, 15))
                .openNote(noteDTO)
                .trash()
                .logout(email);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderAlphabeticallyAZ() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderAlphabeticallyZA() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderOldestModifiedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderNewestModifiedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderOldestCreatedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testPinnedNotesWhileChangingOrderNewestCreatedDate() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

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
                .logout(email);
    }

    @Test
    public void testFlipToEditMode() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .markdown(true)
                .editMode()
                .checkPreviewModeDoesNotSelected()
                .markdown(false);

        new NotePage()
                .trash()
                .logout(email);
    }

    @Test
    public void testFlipToPreviewMode() {

        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .markdown(true)
                .previewMode()
                .checkEditModeDoesNotSelected()
                .markdown(false);

        new NotePage()
                .trash()
                .logout(email);
    }

    @Test
    public void testCancelSearchAndGoBackToMainPage() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUniqueContent(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .openSearchPage()
                .search(DataProvider.RANDOM_SEARCH_PARAMETER)
                .cancelSearchAndGoBack()
                .checkNoteContentIsInTheList(noteDTO.getContent().substring(0, 15))
                .openNote(noteDTO)
                .trash()
                .logout(email);
    }

    @Test
    public void testAddedUrlIsLinkified() {
        NoteDTO noteDTO = DataProvider.generateNotesWithUrl(1).get(0);

        new IntroPage()
                .goToLoginWithEmail()
                .login(email, password);

        new MainPage()
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .focusOnContent()
                .checkUrlIsLinkified()
                .closeActionMode();

        new NotePage()
                .trash()
                .logout(email);
    }
}
