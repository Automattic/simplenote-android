package com.automattic.simplenote.espressoUITests;


import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.logOut;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.login;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithCredentials;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.addNote;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.deleteNoteFromList;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapEmptyTrash;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapNote;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.trash;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.undoDeleteNoteFromList;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteEditorHelpers.optionsTapTrash;
import static com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class NotesActivityTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void addNoteDeleteFromNotesList() {
        loginWithCredentials();
        login(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        addNote();
        sleep(2, SECONDS);
        deleteNoteFromList();
        undoDeleteNoteFromList();
        deleteNoteFromList();
        logOut();
    }

    //adds note > delete > undo deletion > deletes note from all notes list > logout

    @Test
    public void addNoteDeleteNoteFromDetail() {
        loginWithCredentials();
        login(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        addNote();
        tapNote();
        optionsTapTrash();
        logOut();
    }

    //adds note > goes to note detail > delete > logout

    @Test
    public void emptyTrash() {
        loginWithCredentials();
        login(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        addNote();
        tapNote();
        optionsTapTrash();
        trash();
        sleep(2, SECONDS);
        tapEmptyTrash();
        logOut();
    }

    //adds note > delete note > trash > empty trash > verify trash is empty > logout

}

