package com.automattic.simplenote;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.automattic.simplenote.utils.EspressoUITests.addChecklist;
import static com.automattic.simplenote.utils.EspressoUITests.addNote;
import static com.automattic.simplenote.utils.EspressoUITests.logOut;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithCredentials;
import static com.automattic.simplenote.utils.EspressoUITests.loginWithValidCredentials;
import static com.automattic.simplenote.utils.EspressoUITests.tapNote;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class NoteDetailOptions {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void addChecklistNote() throws InterruptedException {
        loginWithCredentials();
        loginWithValidCredentials();
        addNote();
        Thread.sleep(2000);
        tapNote();
        addChecklist();
        Espresso.pressBack();
        logOut();
    }

}

