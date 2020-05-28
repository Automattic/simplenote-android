package com.automattic.simplenote;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.automattic.simplenote.utils.EspressoUITests.addNote;
import static com.automattic.simplenote.utils.EspressoUITests.deleteNoteFromList;
import static com.automattic.simplenote.utils.EspressoUITests.logOut2;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithCredentials;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithValidCredentials;
import static com.automattic.simplenote.utils.EspressoUITests.optionsTapTrash;
import static com.automattic.simplenote.utils.EspressoUITests.tapNote;
import static com.automattic.simplenote.utils.EspressoUITests.undoDeleteNoteFromList;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class DeleteNotes {

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
        logOut2();
    }

    //adds note > delete > undo deletion > deletes note from all notes list > logout

    @Test
    public void addNoteDeleteNoteFromDetail() throws InterruptedException {
        loginWithCredentials();
        loginWithValidCredentials();
        addNote();
        tapNote();
        optionsTapTrash();
        logOut2();
    }

    //adds note > goes to note detail > delete > logout

    //TODO: Add a test flow emptying trash

}

