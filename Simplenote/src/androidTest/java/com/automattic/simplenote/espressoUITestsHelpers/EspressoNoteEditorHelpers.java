package com.automattic.simplenote.espressoUITestsHelpers;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn;
import static com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

public class EspressoNoteEditorHelpers {

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public static void addChecklist() {
        clickOn(R.id.menu_checklist);
    }

    @Test
    public static void tapInformationButton() {
        clickOn(R.id.menu_info);
    }

    @Test
    public static void moreOptions() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
        onView(withId(R.string.more_options)).perform(click());
    }

    @Test
    public static void optionsTapTrash() {
        onView(withContentDescription(R.string.more_options))
                .perform(click());
        sleep(2, SECONDS);
        onView(withText("Trash")).perform(click()); //when using ID or position crashes on popup window menu test fails
        sleep(2, SECONDS);
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
}