package com.automattic.simplenote.espressoUITests;


import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.logOut;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.login;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithCredentials;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.addNote;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapNote;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteEditorHelpers.addChecklist;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class NoteEditorOptionsTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void addChecklistNote() throws InterruptedException {
        loginWithCredentials();
        login(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        addNote();
        Thread.sleep(2000);
        tapNote();
        addChecklist();
        Espresso.pressBack();
        logOut();
    }

}

