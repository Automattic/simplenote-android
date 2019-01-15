package com.automattic.simplenote;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;
import com.simperium.client.Query;

public class PinnedNoteWidgetConfigureActivity extends AppCompatActivity {

    private Bucket<Note> mNotesBucket;
    private NotesCursorAdapter mCursorAdapter;

    public PinnedNoteWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.pinned_note_widget_configure);

        // Configure toolbar.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.select_a_note);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Query and load notes into list.
        Simplenote currentApp = (Simplenote) getApplication();
        mNotesBucket = currentApp.getNotesBucket();
        Query<Note> query = Note.all(mNotesBucket);
        query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        Cursor cursor = query.execute();
        mCursorAdapter = new NotesCursorAdapter(this, cursor);
        ListView lv = findViewById(R.id.lista);
        lv.setAdapter(mCursorAdapter);
    }

    public class NotesCursorAdapter extends CursorAdapter {
        public NotesCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.note_list_row, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView titleTextView = view.findViewById(R.id.note_title);
            TextView contentTextView = view.findViewById(R.id.note_content);
            ToggleButton toggleView = view.findViewById(R.id.pin_button);
            // Extract properties from cursor
            String title = "";
            String snippet = "";
            if(cursor.getColumnIndex(Note.TITLE_INDEX_NAME) > -1) title =  cursor.getString(cursor.getColumnIndex(Note.TITLE_INDEX_NAME));
            if(cursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME) > -1) snippet =  cursor.getString(cursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME));
            // Populate fields with extracted properties
            titleTextView.setText(title);
            contentTextView.setText(snippet);
            toggleView.setVisibility(View.GONE);

        }
    }
}

