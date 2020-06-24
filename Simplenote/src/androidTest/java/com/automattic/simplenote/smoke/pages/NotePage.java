package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.data.NoteDTO;
import com.automattic.simplenote.smoke.utils.TestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;


public class NotePage extends BasePage {

    private static final Integer INPUT_CONTENT = R.id.note_content;
    private static final Integer INPUT_TAG = R.id.tag_input;
    private static final String TEXT_BUTTON_EDIT = "EDIT";
    private static final String TEXT_BUTTON_PREVIEW = "PREVIEW";
    private static final Integer BUTTON_CLOSE_ACTION_MODE = R.id.action_mode_close_button;

    private OptionsMenu optionsMenu;

    public NotePage() {
        optionsMenu = new OptionsMenu();
    }

    public NotePage enterNewNote(NoteDTO noteDTO) {
        TestUtils.idleForAShortPeriod();
        enterText(INPUT_CONTENT, noteDTO.getTitle() + "\n" + noteDTO.getContent());
        for (String tag : noteDTO.getTags()) {
            enterText(INPUT_TAG, tag);
        }
        TestUtils.idleForAShortPeriod();
        return this;
    }

    public MainPage trash() {
        optionsMenu.trash();
        TestUtils.idleForAShortPeriod();

        return new MainPage();
    }

    public NotePage markdown(boolean state) {
        optionsMenu.markdown(state);

        return this;
    }

    public NotePage previewMode() {
        clickButton(TEXT_BUTTON_PREVIEW);

        return this;
    }

    public NotePage editMode() {
        waitForElementToBeDisplayed(R.id.tag_input);
        clickButton(TEXT_BUTTON_EDIT);

        return this;
    }

    public NotePage checkEditModeDoesNotSelected() {
        onView(
                allOf(
                        withText(TEXT_BUTTON_EDIT),
                        isDisplayed(),
                        isSelected()
                )
        ).check(doesNotExist());

        return this;
    }

    public NotePage checkPreviewModeDoesNotSelected() {
        onView(
                allOf(
                        withText(TEXT_BUTTON_PREVIEW),
                        isDisplayed(),
                        isSelected()
                )
        ).check(doesNotExist());

        return this;
    }

    public NotePage checkUrlIsLinkified() {
        visibleElementWithId(R.id.action_bar_title);
        visibleElementWithId(R.id.action_bar_title);

        return this;
    }

    public NotePage closeActionMode() {
        clickButton(BUTTON_CLOSE_ACTION_MODE);

        return this;
    }

    public NotePage focusOnContent() {
        clickButton(INPUT_CONTENT);

        return this;
    }

    public TrashPage restore() {
        optionsMenu.restore();
        TestUtils.idleForAShortPeriod();

        return new TrashPage();
    }

    public NotePage switchPinMode() {
        optionsMenu.switchPinMode();
        TestUtils.idleForAShortPeriod();

        return this;
    }
}
