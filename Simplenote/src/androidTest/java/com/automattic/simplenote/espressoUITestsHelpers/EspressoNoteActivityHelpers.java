package com.automattic.simplenote.espressoUITestsHelpers;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.Espresso;
import androidx.test.rule.ActivityTestRule;

import static com.automattic.simplenote.espressoUITestsHelpers.EspressoGeneralHelpers.tapNoteButton;
import static com.automattic.simplenote.espressoUITestsHelpers.EspressoNoteEditorHelpers.moreOptions;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer;
import static com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.schibsted.spain.barista.interaction.BaristaKeyboardInteractions.closeKeyboard;
import static com.schibsted.spain.barista.interaction.BaristaListInteractions.clickListItem;
import static com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

public class EspressoNoteActivityHelpers {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public static void tapAddNoteButton() {
        tapNoteButton();
        sleep(2, SECONDS);
    }

    @Test
    public static void writeNoteEditor(String noteContent) {
        writeTo(R.id.note_content, noteContent);
    }

    @Test
    public static void exitNoteEditor() {
        closeKeyboard();
        Espresso.pressBack();
        sleep(2, SECONDS);
    }

    @Test
    public static void assert1noteSelected() {
        assertContains("1 note selected"); //using text instead of string because the string itself includes "%d note selected" and that's not the visible copy, the visible copy includes the notes number such as "1 note selected"
        sleep(2, SECONDS);
    }

    @Test
    public static void deleteNoteFromList() {
        clickOn(R.id.menu_trash);
        assertContains(R.string.note_deleted);
    }

    @Test
    public static void tapMenuTrash() {
        clickOn(R.id.menu_trash);
    }

    @Test
    public static void undoDeleteNoteFromList() {
        clickOn(R.string.undo);
    }

    @Test
    public static void tapSearchNotes() {
        clickOn(R.id.menu_search);
    }

    @Test
    public static void performSearch() {
        clickOn(R.id.search_src_text);
    }

    @Test
    public static void tapPin() {
        moreOptions();
        clickOn(R.string.toggle_pin);
    }

    @Test
    public static void openDrawerTapTrash() {
        openDrawer();
        clickListItem(R.id.design_navigation_view, 2);
    }

    @Test
    public static void allNotes() {
        openDrawer();
        clickListItem(R.id.design_navigation_view, 1);
    }

    @Test
    public static void tapEmptyTrash() {
        sleep(2, SECONDS);
        clickOn(R.id.menu_empty_trash);
        clickOn(R.string.yes);
        assertContains(R.string.empty_notes_trash);
    }

}