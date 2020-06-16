package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.data.NoteDTO;


public class NotePage extends BasePage {

    private static final Integer INPUT_CONTENT = R.id.note_content;
    private static final Integer INPUT_TAG = R.id.tag_input;

    public NotePage enterNewNote(NoteDTO noteDTO) {
        enterText(INPUT_CONTENT, noteDTO.getTitle() + "\\n" + noteDTO.getContent());
        for (String tag : noteDTO.getTags()) {
            enterText(INPUT_TAG, tag);
        }

        return this;
    }
}
