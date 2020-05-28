package com.automattic.simplenote.espressoUITests;


import com.automattic.simplenote.NotesActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.logOut;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithCredentials;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithValidCredentials;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.addNote;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.deleteNoteFromList;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapEmptyTrash;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapNote;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.trash;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.undoDeleteNoteFromList;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteEditorHelpers.optionsTapTrash;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class NotesActivityTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void addNoteDeleteFromNotesList() throws InterruptedException {
        loginWithCredentials();
        loginWithValidCredentials();
        addNote();
        Thread.sleep(2000);
        deleteNoteFromList();
        undoDeleteNoteFromList();
        deleteNoteFromList();
        logOut();
    }

    //adds note > delete > undo deletion > deletes note from all notes list > logout

    @Test
    public void addNoteDeleteNoteFromDetail() throws InterruptedException {
        loginWithCredentials();
        loginWithValidCredentials();
        addNote();
        tapNote();
        optionsTapTrash();
        logOut();
    }

    //adds note > goes to note detail > delete > logout

    @Test
    public void emptyTrash() throws InterruptedException {
        loginWithCredentials();
        loginWithValidCredentials();
        addNote();
        tapNote();
        optionsTapTrash();
        trash();
        Thread.sleep(2000);
        tapEmptyTrash();
        logOut();
    }

    //adds note > delete note > trash > empty trash > verify trash is empty > logout

}

