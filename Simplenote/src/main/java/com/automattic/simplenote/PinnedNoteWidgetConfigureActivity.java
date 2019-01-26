package com.automattic.simplenote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.Query;

public class PinnedNoteWidgetConfigureActivity extends AppCompatActivity {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private NotesCursorAdapter mNotesAdapter;
    private AppWidgetManager widgetManager;
    private RemoteViews views;

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

        // Get widget information
        widgetManager = AppWidgetManager.getInstance(this);
        views = new RemoteViews(this.getPackageName(), R.layout.pinned_note_widget);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Configure toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.select_a_note);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Query and load notes into list
        Simplenote currentApp = (Simplenote) getApplication();
        Bucket<Note> mNotesBucket = currentApp.getNotesBucket();
        Query<Note> query = Note.all(mNotesBucket);
        query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        ObjectCursor<Note> cursor = query.execute();
        mNotesAdapter = new NotesCursorAdapter(this, cursor);
        ListView lv = findViewById(R.id.list);
        lv.setAdapter(mNotesAdapter);
    }

    private class NotesCursorAdapter extends CursorAdapter {
        private final ObjectCursor<Note> mCursor;

        private NotesCursorAdapter(Context context, ObjectCursor<Note> cursor) {
            super(context, cursor, 0);
            mCursor = cursor;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.note_list_row, parent, false);
        }

        @Override
        public Note getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getObject();
        }

        @Override
        public void bindView(View view, final Context context, final Cursor cursor) {
            view.setTag(cursor.getPosition());
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

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get the selected note
                    Note note = mNotesAdapter.getItem((int)view.getTag());

                    // Store link between note and widget in SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("com.automattic.simplenote", Context.MODE_PRIVATE);
                    prefs.edit().putString(Integer.toString(mAppWidgetId), note.getSimperiumKey()).apply();

                    // Prepare bundle for NoteEditorActivity
                    Bundle arguments = new Bundle();
                    arguments.putString(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
                    arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());

                    // Create intent to navigate to selected note on widget click
                    Intent intent = new Intent(context, NoteEditorActivity.class);
                    intent.putExtras(arguments);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, mAppWidgetId, intent, 0);

                    // Set widget content
                    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                    views.setTextViewText(R.id.widget_text, note.getTitle());
                    views.setImageViewResource(R.id.widget_logo, R.drawable.simplenote_logo);
                    widgetManager.updateAppWidget(mAppWidgetId, views);

                    // Set the result as successful
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }
            });

        }
    }
}

