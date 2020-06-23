package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.data.NoteDTO;
import com.automattic.simplenote.smoke.utils.TestUtils;


public class NotePage extends BasePage {

    private static final Integer INPUT_CONTENT = R.id.note_content;
    private static final Integer INPUT_TAG = R.id.tag_input;

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
