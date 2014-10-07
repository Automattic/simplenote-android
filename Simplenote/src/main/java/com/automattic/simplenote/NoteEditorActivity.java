package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.automattic.simplenote.utils.ThemeUtils;
import com.google.analytics.tracking.android.EasyTracker;

public class NoteEditorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeUtils.setTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_editor);

        EasyTracker.getInstance().activityStart(this);

        // No title, please.
        setTitle("");

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        NoteEditorFragment noteEditorFragment;
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

            noteEditorFragment = new NoteEditorFragment();
            noteEditorFragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.note_editor_container, noteEditorFragment, NotesActivity.TAG_NOTE_EDITOR)
                    .commit();
        }
    }
}
