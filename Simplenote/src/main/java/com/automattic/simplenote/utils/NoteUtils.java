package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.automattic.simplenote.NoteEditorActivity;
import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;

import java.text.NumberFormat;
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

    public static String getCharactersCount(String content) {
        return NumberFormat.getInstance().format(content.length());
    }

    public static String getWordCount(String content) {
        int words = (content.trim().length() == 0) ? 0 : content.trim().split("([\\W]+)").length;
        return NumberFormat.getInstance().format(words);
    }

    public static void showDialogDeletePermanently(final Activity activity, final Note note) {
        new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Dialog))
            .setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                R.string.delete,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (note != null) {
                            note.delete();

                            // Show empty placeholder for large devices in landscape.
                            if (activity instanceof NotesActivity) {
                                NotesActivity notesActivity = (NotesActivity) activity;
                                if (notesActivity.getNoteListFragment() != null) {
                                    notesActivity.getNoteListFragment().updateSelectionAfterTrashAction();
                                } else {
                                    notesActivity.showDetailPlaceholder();
                                }
                            // Close editor for small devices and large devices in portrait.
                            } else if (activity instanceof NoteEditorActivity) {
                                ((NoteEditorActivity) activity).finish();
                            }
                        } else {
                            DialogUtils.showDialogWithEmail(
                                activity,
                                activity.getString(R.string.delete_dialog_error_message)
                            );
                        }
                    }
                }
            )
            .show();
    }
}
