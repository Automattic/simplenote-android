package com.automattic.simplenote.utils;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;

import java.util.Calendar;

/**
 * Created by Ondrej Ruttkay on 28/03/2016.
 */
public class NoteUtils {
    public static void setNotePin(Note note, boolean isPinned) {
        if (note != null && isPinned != note.isPinned()) {
            note.setPinned(isPinned);
            note.setModificationDate(Calendar.getInstance());
            note.save();

            AnalyticsTracker.track(
                    isPinned ? AnalyticsTracker.Stat.EDITOR_NOTE_PINNED :
                            AnalyticsTracker.Stat.EDITOR_NOTE_UNPINNED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "pin_button"
            );
        }
    }
}
