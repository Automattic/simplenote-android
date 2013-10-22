package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.google.analytics.tracking.android.EasyTracker;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import java.util.Calendar;

public class NoteEditorActivity extends Activity implements Bucket.Listener<Note> {

    private NoteEditorFragment mNoteEditorFragment;
    private Bucket<Note> mNotesBucket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeUtils.setTheme(this);

        super.onCreate(savedInstanceState);
        Simplenote currentApp = (Simplenote) getApplication();
        mNotesBucket = currentApp.getNotesBucket();
        setContentView(R.layout.activity_note_editor);

        EasyTracker.getInstance().activityStart(this);

        // No title, please.
        setTitle("");

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            // Create the note editor fragment
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID,
                    intent.getStringExtra(NoteEditorFragment.ARG_ITEM_ID));
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE,
                    intent.getBooleanExtra(NoteEditorFragment.ARG_NEW_NOTE, false));
            if (intent.hasExtra(NoteEditorFragment.ARG_MATCH_OFFSETS))
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS,
                    intent.getStringExtra(NoteEditorFragment.ARG_MATCH_OFFSETS));

            mNoteEditorFragment = new NoteEditorFragment();
            mNoteEditorFragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.note_editor_container, mNoteEditorFragment, NotesActivity.TAG_NOTE_EDITOR)
                    .commit();
        } else {
            mNoteEditorFragment = (NoteEditorFragment)getFragmentManager().findFragmentByTag(NotesActivity.TAG_NOTE_EDITOR);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNotesBucket.start();
        mNotesBucket.addOnNetworkChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNotesBucket.removeOnNetworkChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.note_editor, menu);

        MenuItem trashItem = menu.findItem(R.id.menu_delete).setTitle(R.string.undelete);
        if (mNoteEditorFragment.getNote() != null && mNoteEditorFragment.getNote().isDeleted())
            trashItem.setTitle(R.string.undelete);
        else
            trashItem.setTitle(R.string.delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                Note sharedNote = mNoteEditorFragment.getNote();
                if (sharedNote != null) {
                    Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, sharedNote.getContent());
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_note)));
                    EasyTracker.getTracker().sendEvent("note", "shared_note", "action_bar_share_button", null);
                }
                return true;
            case R.id.menu_delete:
                Note deletedNote = mNoteEditorFragment.getNote();
                if (deletedNote != null) {
                    deletedNote.setDeleted(!deletedNote.isDeleted());
                    deletedNote.setModificationDate(Calendar.getInstance());
                    deletedNote.save();
                    Intent resultIntent = new Intent();
                    if (deletedNote.isDeleted())
                        resultIntent.putExtra(Simplenote.DELETED_NOTE_ID, deletedNote.getSimperiumKey());
                    setResult(RESULT_OK, resultIntent);
                }
                finish();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {

    }

    @Override
    public void onChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, final String key) {
        if (changeType == Bucket.ChangeType.MODIFY) {
            final boolean resetEditor = mNoteEditorFragment.getNote() != null && mNoteEditorFragment.getNote().getSimperiumKey().equals(key);
            if (resetEditor) {
                try {
                    final Note updatedNote = mNotesBucket.get(key);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mNoteEditorFragment.updateNote(updatedNote);
                        }
                    });
                } catch (BucketObjectMissingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {

    }
}
