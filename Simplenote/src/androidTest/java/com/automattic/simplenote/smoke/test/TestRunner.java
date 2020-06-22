package com.automattic.simplenote.smoke.test;

import androidx.test.rule.ActivityTestRule;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.smoke.data.DataProvider;
import com.automattic.simplenote.smoke.data.NoteDTO;
import com.automattic.simplenote.smoke.pages.IntroPage;
import com.automattic.simplenote.smoke.pages.LoginPage;
import com.automattic.simplenote.smoke.pages.MainPage;
import com.automattic.simplenote.smoke.pages.SearchPage;
import com.automattic.simplenote.smoke.utils.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRunner {
    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    IntroPage introPage = new IntroPage();
    LoginPage loginPage;
    MainPage mainPage = new MainPage();
    SearchPage searchPage = new SearchPage();

    @Before
    public void setUp() {
        TestUtils.idleForAShortPeriod();
        TestUtils.logoutIfNecessary();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLogin() {
        LoginPage loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);
        mainPage.isOpened();
    }

    @Test
    public void testLogout() {
        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        mainPage.isOpened();
        mainPage.logout();
    }

    @Test
    public void testLoginWithWrongPassword() {
        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_WRONG_PASSWORD);
        loginPage.checkLoginFailedMessage();
    }

    @Test
    public void testSearchingInTheSearchFieldDoesAGlobalSearch() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        String searchParam = noteDTO.getTitle();

        mainPage.addNewNote(noteDTO);

        mainPage.openSearchPage().search(searchParam).checkSearchResultsTitleAndContent(searchParam);

        mainPage.logout();
    }

    @Test
    public void testFilterByTagWhenClickingOnTagInTagDrawer() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        mainPage.addNewNote(noteDTO);

        mainPage.selectTagFromDrawer(noteDTO.getTags().get(0)).checkSearchResultsTitleAndContent(noteDTO.getTitle());

        mainPage.logout();
    }

    @Test
    public void testViewTrashedNotesByClickingOnTrash() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        mainPage
                .addNewNote(noteDTO)
                .openTrashPage()
                .checkNoteIsTrashed(noteDTO.getTitle());

        mainPage.logout();
    }

    @Test
    public void testRestoreNoteFromTrashScreen() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        mainPage
                .addNewNote(noteDTO)
                .openTrashPage()
                .openTrashedNote()
                .restore();

        mainPage.logout();
    }

    @Test
    public void testTrashNote() {
        NoteDTO noteDTO = DataProvider.generateNotes(1).get(0);

        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        mainPage
                .addNewNote(noteDTO)
                .openNote(noteDTO)
                .trash()
                .checkNoteIsNotInTheList(noteDTO);

        mainPage.logout();
    }

    @Test
    public void testShareAnalytics() {

        loginPage = introPage.goToLoginWithEmail();
        loginPage.login(DataProvider.LOGIN_EMAIL, DataProvider.LOGIN_PASSWORD);

        mainPage
                .switchShareAnalytics(true)
                .logout(DataProvider.LOGIN_EMAIL);
    }
}
