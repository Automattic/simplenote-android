package com.automattic.simplenote.espressoUITests;


import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.enterEmailPassword;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.logOut;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithEmail;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.tapLoginButton;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoGeneralHelpers.tapNoteButton;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.assert1noteSelected;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.exitNoteEditor;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapAddNoteButton;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapEmptyTrash;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapMenuTrash;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.openDrawerTapTrash;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.writeNoteEditor;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteEditorHelpers.optionsTapTrash;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class NotesActivityTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void addNoteDeleteFromNotesList() {
        loginWithEmail();
        enterEmailPassword(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        tapLoginButton();
        tapNoteButton();
        writeNoteEditor("writeNoteContent");
        exitNoteEditor();
        onView(withText("writeNoteContent")).perform(longClick());
        assert1noteSelected();
        tapMenuTrash();
        logOut();
    }

    //adds note > delete > undo deletion > deletes note from all notes list > logout

    @Test
    public void addNoteDeleteNoteFromDetail() {
        loginWithEmail();
        enterEmailPassword(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        tapLoginButton();
        tapNoteButton();
        writeNoteEditor("testEspresso");
        exitNoteEditor();
        onView(withText("testEspresso")).perform(click());
        optionsTapTrash();
        logOut();
    }

    //adds note > goes to note detail > delete > logout

    @Test
    public void emptyTrash() {
        loginWithEmail();
        enterEmailPassword(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        tapLoginButton();
        tapAddNoteButton();
        writeNoteEditor("simpleNote001");
        onView(withText("simpleNote001")).check(matches(isDisplayed()));
        onView((withText("simpleNote001"))).perform((click()));
        optionsTapTrash();
        openDrawerTapTrash();
        tapEmptyTrash();
        logOut();
    }

    //adds note > delete note > trash > empty trash > verify trash is empty > logout

}

