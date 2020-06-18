package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

public class TrashPage extends BasePage {

    private static final Integer BUTTON_EMPTY_TRASH = R.id.menu_empty_trash;
    private static final Integer BUTTON_YES = R.id.button1;
    private static final Integer TEXT_NOTE_TITLE = R.id.note_title;

    public TrashPage emptyTrash() {
        clickButton(BUTTON_EMPTY_TRASH);
        clickButton(BUTTON_YES);

        TestUtils.idleForAShortPeriod();

        return this;
    }

    // TODO we are looking the position of 0. If other trashed notes consists that could lead to break the test
    public TrashPage checkNoteIsTrashed(String searchParam) {
        checkElementDisplayedWithTextAtPosition(TEXT_NOTE_TITLE, searchParam, 0);

        return this;
    }

    // TODO we are looking the position of 0. If other trashed notes consists that could lead to break the test
    public NotePage openTrashedNote() {
        TestUtils.idleForAShortPeriod();

        clickElementDisplayedWithTextAtPosition(R.id.note_title, "Corona", 0);

        return new NotePage();
    }
}
