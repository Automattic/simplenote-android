package com.automattic.simplenote;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.FileUtils;
import com.automattic.simplenote.utils.TagUtils;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

public class Importer {
    private final Bucket<Note> mNotesBucket;
    private final Bucket<Tag> mTagsBucket;

    public Importer(Simplenote simplenote) {
        mNotesBucket = simplenote.getNotesBucket();
        mTagsBucket = simplenote.getTagsBucket();
    }

    public static void fromUri(FragmentActivity activity, Uri uri) throws ImportException {
        try {
            String fileType = FileUtils.getFileExtension(activity, uri);
            new Importer((Simplenote) activity.getApplication())
                    .dispatchFileImport(
                            fileType,
                            FileUtils.readFile(activity, uri)
                    );

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.SETTINGS_IMPORT_NOTES,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "import_notes_type_" + fileType
            );
        } catch (IOException e) {
            throw new ImportException(FailureReason.FileError);
        }
    }

    private void dispatchFileImport(String fileType, String content) throws ImportException {
        switch (fileType) {
            case "json":
                importJsonFile(content);
                break;
            case "md":
                importMarkdown(content);
                break;
            case "txt":
                importPlaintext(content);
                break;
            default:
                throw new ImportException(FailureReason.UnknownExportType);
        }
    }

    private void importPlaintext(String content) {
        addNote(Note.fromContent(mNotesBucket, content));
    }

    private void importMarkdown(String content) {
        Note note = Note.fromContent(mNotesBucket, content);
        note.enableMarkdown();

        addNote(note);
    }

    private void addNote(Note note) {
        for (String tagName : note.getTags()) {
            try {
                TagUtils.createTagIfMissing(mTagsBucket, tagName);
            } catch (BucketObjectNameInvalid e) {
                // if it can't be added then remove it, we can't keep it anyway
                note.removeTag(tagName);
            }
        }

        note.save();
    }

    private void importJsonFile(String content) throws ImportException {
        try {
            importJsonExport(new JSONObject(content));
        } catch (JSONException | ParseException e) {
            throw new ImportException(FailureReason.ParseError);
        }
    }

    private void importJsonExport(JSONObject export) throws JSONException, ParseException {
        JSONArray activeNotes = export.optJSONArray("activeNotes");
        JSONArray trashedNotes = export.optJSONArray("trashedNotes");

        ArrayList<Note> notesList = new ArrayList<>();

        for (int i = 0; activeNotes != null && i < activeNotes.length(); i++) {
            Note note = Note.fromExportedJson(mNotesBucket, activeNotes.getJSONObject(i));
            notesList.add(note);
        }

        for (int j = 0; trashedNotes != null && j < trashedNotes.length(); j++) {
            Note note = Note.fromExportedJson(mNotesBucket, trashedNotes.getJSONObject(j));
            note.setDeleted(true);

            notesList.add(note);
        }

        for (Note note : notesList) {
            addNote(note);
        }
    }

    public enum FailureReason {
        FileError,
        UnknownExportType,
        ParseError
    }

    public static class ImportException extends Exception {
        private FailureReason mReason;

        ImportException(FailureReason reason) {
            mReason = reason;
        }

        public FailureReason getReason() {
            return mReason;
        }
    }
}
