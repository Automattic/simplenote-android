package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.automattic.simplenote.NoteEditorActivity;
import com.automattic.simplenote.NoteEditorFragment;
import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import java.util.regex.Pattern;

public class SimplenoteLinkify {
    public static final String SIMPLENOTE_SCHEME = "simplenote://";
    public static final String SIMPLENOTE_LINK_PREFIX = SIMPLENOTE_SCHEME + "note/";
    public static final String SIMPLENOTE_LINK_ID = "([a-zA-Z0-9_\\.\\-%@]{1,256})";
    public static final Pattern SIMPLENOTE_LINK_PATTERN = Pattern.compile(SIMPLENOTE_LINK_PREFIX + SIMPLENOTE_LINK_ID);

    // Works the same as Linkify.addLinks, but doesn't set movement method
    public static boolean addLinks(TextView text, int mask) {
        if (mask == 0) {
            return false;
        }

        CharSequence t = text.getText();

        if (t instanceof Spannable) {
            boolean linked = Linkify.addLinks((Spannable) t, mask);
            Linkify.addLinks((Spannable) t, SIMPLENOTE_LINK_PATTERN, SIMPLENOTE_SCHEME);

            return linked;
        } else {
            SpannableString s = SpannableString.valueOf(t);

            if (Linkify.addLinks(s, mask)) {
                text.setText(s);
                return true;
            }

            return false;
        }
    }

    public static String getNoteLink(String id) {
        return "(" + SIMPLENOTE_LINK_PREFIX + id + ")";
    }

    public static String getNoteLinkWithTitle(String title, String id) {
        return "[" + title + "]" + getNoteLink(id);
    }

    public static void openNote(Activity activity, String id) {
        Bucket<Note> bucket = ((Simplenote) activity.getApplication()).getNotesBucket();

        try {
            Note note = bucket.get(id);

            if (activity instanceof NotesActivity) {
                ((NotesActivity) activity).selectDefaultTag();
                ((NotesActivity) activity).onNoteSelected(note.getSimperiumKey(), null, note.isMarkdownEnabled(), note.isPreviewEnabled());
                ((NotesActivity) activity).scrollToSelectedNote(note.getSimperiumKey());
            } else if (activity instanceof NoteEditorActivity) {
                Intent intent = new Intent(activity, NoteEditorActivity.class);
                intent.putExtra(NoteEditorFragment.ARG_IS_FROM_WIDGET, false);
                intent.putExtra(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
                intent.putExtra(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());
                intent.putExtra(NoteEditorFragment.ARG_PREVIEW_ENABLED, note.isPreviewEnabled());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                activity.finish();
            } else {
                Toast.makeText(activity, R.string.open_note_error, Toast.LENGTH_SHORT).show();
                Log.d("openNote", "Activity is not NotesActivity nor NoteEditorActivity");
            }
        } catch (BucketObjectMissingException e) {
            Toast.makeText(activity, R.string.open_note_error, Toast.LENGTH_SHORT).show();
            Log.d("openNote", activity.getString(R.string.open_note_error));
        }
    }
}
