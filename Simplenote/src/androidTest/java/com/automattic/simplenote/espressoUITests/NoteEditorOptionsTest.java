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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.enterApp;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.enterEmailPassword;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.logOut;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoAuthenticationHelpers.loginWithEmail;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.exitNoteEditor;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.tapAddNoteButton;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteActivityHelpers.writeNoteEditor;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteEditorHelpers.addChecklist;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class NoteEditorOptionsTest {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void addChecklistNote() {
        loginWithEmail();
        enterEmailPassword(BuildConfig.TEST_USER_EMAIL,BuildConfig.TEST_USER_PASSWORD);
        enterApp();
        tapAddNoteButton();
        writeNoteEditor("noteContent001");
        exitNoteEditor();
        onView(withText("noteContent001")).check(matches(isDisplayed()));
        onView((withText("noteContent001"))).perform((click()));
        addChecklist();
        Espresso.pressBack();
        logOut();
    }

}

