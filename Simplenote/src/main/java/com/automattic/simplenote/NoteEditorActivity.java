package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.automattic.simplenote.utils.ThemeUtils;

import org.wordpress.passcodelock.AppLockManager;

public class NoteEditorActivity extends ActionBarActivity {
    public static final String ARG_IS_SHARED_NOTE = "arg_is_shared_note";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_editor);

        // No title, please.
        setTitle("");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        NoteEditorFragment noteEditorFragment;
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            // Create the note editor fragment
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID,
                    intent.getStringExtra(NoteEditorFragment.ARG_ITEM_ID));

            boolean isNewNote = intent.getBooleanExtra(NoteEditorFragment.ARG_NEW_NOTE, false);
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, isNewNote);
            if (intent.hasExtra(NoteEditorFragment.ARG_MATCH_OFFSETS)) {
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS,
                        intent.getStringExtra(NoteEditorFragment.ARG_MATCH_OFFSETS));
            }

            if (intent.getBooleanExtra(ARG_IS_SHARED_NOTE, false)) {
                // Set timeout to 0 to force showing of pin lock if it is enabled
                AppLockManager.getInstance().getCurrentAppLock().setOneTimeTimeout(0);
            }

            noteEditorFragment = new NoteEditorFragment();
            noteEditorFragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.note_editor_container, noteEditorFragment, NotesActivity.TAG_NOTE_EDITOR)
                    .commit();
        }
    }
}
