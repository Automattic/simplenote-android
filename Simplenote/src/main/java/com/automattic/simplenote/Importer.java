package com.automattic.simplenote;

import android.net.Uri;

import androidx.fragment.app.Fragment;

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

public class Importer {
    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;

    public Importer(Simplenote simplenote) {
        mNotesBucket = simplenote.getNotesBucket();
        mTagsBucket = simplenote.getTagsBucket();
    }

    public static void fromUri(Fragment fragment, Uri uri) throws ImportException {
        try {
            new Importer((Simplenote) fragment.getActivity().getApplication())
                    .dispatchFileImport(
                            FileUtils.getFileExtension(fragment.requireContext(), uri),
                            FileUtils.readFile(fragment.requireContext(), uri)
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
        addNote(Note.fromContent(content));
    }

    private void importMarkdown(String content) {
        Note note = Note.fromContent(content);
        note.enableMarkdown();

        addNote(note);
    }

    private void addNote(Note note) {
        // @TODO: Is there a reason to add the tags for deleted notes?
        //        If we un-trash a note then we _do_ want the tags to exist
        //        so for now we're _always_ creating the tags.
        for (String tagName : note.getTags()) {
            try {
                TagUtils.createTagIfMissing(mTagsBucket, tagName);
            } catch (BucketObjectNameInvalid e) {
                // if it can't be added then remove it, we can't keep it anyway
                note.removeTag(tagName);
            }
        }

        mNotesBucket.add(note);
        note.save();
    }

    private void importJsonFile(String content) throws ImportException {
        try {
            importJsonExport(new JSONObject(content));
        } catch (JSONException | ParseException e) {
            throw new ImportException(FailureReason.ParseError);
        }
    }

    // @TODO: how should we fail on parsing?
    //          - parse the full document and fail on _any_ failure, before importing any notes
    //          - parse each note as we go and fail on _any_ failure, after importing some notes
    //          - parse each note as we go, skipping any failure, importing all other notes
    private void importJsonExport(JSONObject export) throws JSONException, ParseException {
        JSONArray activeNotes = export.optJSONArray("activeNotes");
        JSONArray trashedNotes = export.optJSONArray("trashedNotes");

        for (int i = 0; activeNotes != null && i < activeNotes.length(); i++) {
            addNote(Note.fromExportedJson(activeNotes.getJSONObject(i)));
        }

        for (int j = 0; trashedNotes != null && j < trashedNotes.length(); j++) {
            Note note = Note.fromExportedJson(trashedNotes.getJSONObject(j));
            note.setDeleted(true);

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
