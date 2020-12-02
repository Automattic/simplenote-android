package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

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
import java.util.Objects;

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
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(
                R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (note != null) {
                            note.delete();

                            // Show empty placeholder for large devices in landscape.
                            if (activity instanceof NotesActivity) {
                                ((NotesActivity) activity).showDetailPlaceholder();
                            // Close editor for small devices and large devices in portrait.
                            } else if (activity instanceof NoteEditorActivity) {
                                ((NoteEditorActivity) activity).finish();
                            }
                        } else {
                            showDialogErrorDelete(activity);
                        }
                    }
                }
            )
            .show();
    }

    private static void showDialogErrorDelete(Context context) {
        final AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.Dialog))
            .setTitle(R.string.error)
            .setMessage(HtmlCompat.fromHtml(String.format(
                context.getString(R.string.delete_dialog_error_message),
                context.getString(R.string.delete_dialog_error_message_email),
                "<span style=\"color:#",
                Integer.toHexString(ThemeUtils.getColorFromAttribute(context, R.attr.colorAccent) & 0xffffff),
                "\">",
                "</span>"
            )))
            .setPositiveButton(android.R.string.ok, null)
            .show();
        ((TextView) Objects.requireNonNull(dialog.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
