package com.automattic.simplenote.utils;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer;
import static com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.schibsted.spain.barista.interaction.BaristaKeyboardInteractions.closeKeyboard;
import static com.schibsted.spain.barista.interaction.BaristaListInteractions.clickListItem;
import static java.lang.Thread.sleep;

public class EspressoUITests {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public static void loginWithCredentials() throws InterruptedException {
        clickOn(R.id.button_login);
        clickOn(R.id.button_email);
        sleep(2000);
    }

    @Test
    public static void loginWithValidCredentials() throws InterruptedException {
        writeTo(R.id.input_email, BuildConfig.TEST_USER_EMAIL);
        writeTo(R.id.input_password, BuildConfig.TEST_USER_PASSWORD);
        clickOn(R.id.button);
        sleep(2000);
    }

    @Test
    public static void loginWithInvalidCredentials() throws InterruptedException {
        writeTo(R.id.input_email, "test.espresso.00005@gmail.com");
        writeTo(R.id.input_password, "*-Re7]J4Ux8q)g?X");
        clickOn(R.id.button);
        sleep(2000);
    }

    @Test
    public static void logOut() throws InterruptedException {
        openDrawer();
        clickListItem(R.id.design_navigation_view, 3);
        sleep(2000);
        swipeToBottom();
        clickOn(R.string.log_out);
        try {
            onView(withText(R.string.unsynced_notes_message)).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform((click()));
        } catch (NoMatchingViewException e) {
        }
    }
    
    @Test
    public static void tapNoteButton() {
        clickOn(R.id.fab_button);
    }

    @Test
    public static void addNoteContent() {
        writeTo(R.id.note_content, "*-Re7]J4Ux8q)g?X");
    }

    @Test
    public static void addNote() {
        tapNoteButton();
        addNoteContent();
        closeKeyboard();
        Espresso.pressBack();
        assertContains("*-Re7]J4Ux8q)g?X");
    }

    @Test
    public static void deleteNoteFromList() {
        onView(withText("*-Re7]J4Ux8q)g?X")).perform(longClick());
        assertContains("1 note selected"); //using text instead of string because the string itself includes "%d note selected" and that's not the visible copy, the visible copy includes the notes number such as "1 note selected"
        clickOn(R.id.menu_trash);
        assertContains(R.string.note_deleted);
    }

    @Test
    public static void undoDeleteNoteFromList() {
        clickOn(R.string.undo);
        assertContains("*-Re7]J4Ux8q)g?X");
    }

    @Test
    public static void tapNote() {
        clickOn("*-Re7]J4Ux8q)g?X");
    }

    @Test
    public static void addChecklist() {
        clickOn(R.id.menu_checklist);
    }

    @Test
    public static void tapInformationButton() {
        clickOn(R.id.menu_info);
    }

    @Test
    public static void tapOutside() {
        clickOn(R.id.touch_outside);
    }

    @Test
    public static void moreOptions() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
        onView(withId(R.string.more_options)).perform(click());
    }

    @Test
    public static void optionsTapTrash() throws InterruptedException {
        onView(withContentDescription(R.string.more_options))
                .perform(click());
        Thread.sleep(2000);
        onView(withText("Trash")).perform(click()); //when using ID or position crashes on popup window menu test fails
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
    public static void tapMarkdown() {
        moreOptions();
        clickOn(R.id.markdown);
    }

    @Test
    public static void tapShare() {
        moreOptions();
        clickOn(R.string.share);
    }

    @Test
    public static void emptyHistory() {
        moreOptions();
        clickOn(R.string.history);
        assertContains(R.string.error_history);
    }

    @Test
    public static void tapTrashOptions() {
        moreOptions();
        clickOn(R.string.trash);
    }

    @Test
    public static void trash() {
        openDrawer();
        clickListItem(R.id.design_navigation_view, 2);
    }

    @Test
    public static void allNotes() {
        openDrawer();
        clickListItem(R.id.design_navigation_view, 1);
    }

    @Test
    public static void emptyTrash() {
        clickOn(R.id.menu_empty_trash);
        onView(withId(android.R.id.button1)).perform((click()));
        assertContains(R.string.empty_notes_trash);
    }

    public static void swipe(int start, int end, int delay) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        Instrumentation inst = getInstrumentation();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 500, start, 0);
        inst.sendPointerSync(event);
        eventTime = SystemClock.uptimeMillis() + delay;
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, 500, end, 0);
        inst.sendPointerSync(event);
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 500, end, 0);
        inst.sendPointerSync(event);
        SystemClock.sleep(2000);
    }

    // Swipe all the way to the bottom of the screen
    public static void swipeToBottom() {
        swipe(1000, 100, 0);
    }

    // Swipe down one page at a time
    public static void scrollSlowlyDown() {
        swipe(775, 100, 100);
    }

    // Swipe to the top
    public static void swipeToTop() {
        swipe(100, 1000, 0);
    }

    // Swipe up one page at a time
    public static void scrollSlowlyUp() {
        swipe(100, 775, 100);
    }

}