package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Intent;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;

import java.util.Calendar;

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

    public static void deleteNote(Note note, Activity activity) {
        if (note != null) {
            note.setDeleted(!note.isDeleted());
            note.setModificationDate(Calendar.getInstance());
            note.save();
            Intent resultIntent = new Intent();
            if (note.isDeleted()) {
                resultIntent.putExtra(Simplenote.DELETED_NOTE_ID, note.getSimperiumKey());
            }
            activity.setResult(Activity.RESULT_OK, resultIntent);

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_NOTE_DELETED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "trash_menu_item"
            );
        }
    }
}
