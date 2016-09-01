package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

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

    public static void removeNoteReminder(String noteId) {
        new removeRemindNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteId);
    }

    private static class removeRemindNoteTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... args) {
            String noteID = args[0];
            Bucket<Note> notesBucket = Simplenote.getApp().getNotesBucket();
            try {
                Note note = notesBucket.get(noteID);
                note.setReminder(false);
                note.setSnoozeDate(0);
                note.save();
            } catch (BucketObjectMissingException e) {
                // TODO: Handle a missing note
            }
            return null;
        }
    }

    public static void updateSnoozeDateInNote(String noteId, String snoozeDate) {
        new updateSnoozeDateInNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteId, snoozeDate);
    }

    private static class updateSnoozeDateInNoteTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... args) {
            String noteID = args[0];
            Bucket<Note> notesBucket = Simplenote.getApp().getNotesBucket();
            try {
                Note note = notesBucket.get(noteID);
                note.setSnoozeDate(Long.valueOf(args[1]));
                note.save();
            } catch (BucketObjectMissingException e) {
                // TODO: Handle a missing note
            }
            return null;
        }
    }
}
