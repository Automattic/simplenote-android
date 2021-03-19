package com.automattic.simplenote;

import android.net.Uri;

import androidx.fragment.app.Fragment;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.FileUtils;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Importer {
    private Bucket<Note> mNotesBucket;

    public Importer(Simplenote simplenote) {
        mNotesBucket = simplenote.getNotesBucket();
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
                importPlaintext(content, true);
                break;
            case "txt":
                importPlaintext(content, false);
                break;
            default:
                throw new ImportException(FailureReason.UnknownExportType);
        }
    }

    private void importPlaintext(String content, boolean isMarkdownEnabled) {
        Note note = mNotesBucket.newObject();
        note.setMarkdownEnabled(isMarkdownEnabled);
        note.setContent(content);
        note.save();
    }

    private void addNote(JSONObject properties) {
        try {
            mNotesBucket.insertObject(mNotesBucket.newObject().getSimperiumKey(), properties).save();
        } catch (BucketObjectNameInvalid bucketObjectNameInvalid) {
            // this won't happen because mNotesBucket.newObject() generates unused identifier
        }
    }

    private void importJsonFile(String content) throws ImportException {
        try {
            importJsonExport(new JSONObject(content));
        } catch (JSONException e) {
            throw new ImportException(FailureReason.ParseError);
        }
    }

    // @TODO: how should we fail on parsing?
    //          - parse the full document and fail on _any_ failure, before importing any notes
    //          - parse each note as we go and fail on _any_ failure, after importing some notes
    //          - parse each note as we go, skipping any failure, importing all other notes
    private void importJsonExport(JSONObject export) throws JSONException {
        JSONArray activeNotes = export.optJSONArray("activeNotes");
        JSONArray trashedNotes = export.optJSONArray("trashedNotes");

        for (int i = 0; activeNotes != null && i < activeNotes.length(); i++) {
            addNote(activeNotes.getJSONObject(i));
        }

        for (int j = 0; trashedNotes != null && j < trashedNotes.length(); j++) {
            addNote(trashedNotes.getJSONObject(j));
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
