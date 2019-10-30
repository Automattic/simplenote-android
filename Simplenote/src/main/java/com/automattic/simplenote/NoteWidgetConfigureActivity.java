package com.automattic.simplenote;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.Query;
import com.simperium.client.User;

import static com.automattic.simplenote.NoteWidget.KEY_WIDGET_CLICK;
import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_WIDGET;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_NOTE_NOT_FOUND_TAPPED;

public class NoteWidgetConfigureActivity extends AppCompatActivity {
    private AppWidgetManager mWidgetManager;
    private NotesCursorAdapter mNotesAdapter;
    private RemoteViews mRemoteViews;
    private Simplenote mApplication;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public NoteWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.note_widget_configure);

        // Verify user authentication.
        mApplication = (Simplenote) getApplicationContext();
        Simperium simperium = mApplication.getSimperium();
        User user = simperium.getUser();

        if (user.getStatus().equals(User.Status.NOT_AUTHORIZED)) {
            Toast.makeText(NoteWidgetConfigureActivity.this, R.string.sign_in_add_widget, Toast.LENGTH_LONG).show();
            finish();
        }

        // Get widget information
        mWidgetManager = AppWidgetManager.getInstance(NoteWidgetConfigureActivity.this);
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.note_widget);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        showDialog();

        if (intent.hasExtra(KEY_WIDGET_CLICK) && intent.getExtras() != null &&
            intent.getExtras().getSerializable(KEY_WIDGET_CLICK) == NOTE_WIDGET_NOTE_NOT_FOUND_TAPPED) {
            AnalyticsTracker.track(
                NOTE_WIDGET_NOTE_NOT_FOUND_TAPPED,
                CATEGORY_WIDGET,
                "note_widget_note_not_found_tapped"
            );
        }
    }

    private void showDialog() {
        Bucket<Note> mNotesBucket = mApplication.getNotesBucket();
        Query<Note> query = Note.all(mNotesBucket);
        query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        PrefUtils.sortNoteQuery(query, NoteWidgetConfigureActivity.this, true);
        ObjectCursor<Note> cursor = query.execute();

        Context context = new ContextThemeWrapper(NoteWidgetConfigureActivity.this, R.style.Theme_Transparent);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams")
        final View layout = LayoutInflater.from(context).inflate(R.layout.note_widget_configure_list, null);
        final ListView list = layout.findViewById(R.id.list);
        mNotesAdapter = new NotesCursorAdapter(NoteWidgetConfigureActivity.this, cursor);
        list.setAdapter(mNotesAdapter);

        builder.setView(layout)
            .setTitle(R.string.select_note)
            .setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                }
            )
            .setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }
            )
            .show();
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
            String title = "";
            String snippet = "";

            if (cursor.getColumnIndex(Note.TITLE_INDEX_NAME) > -1) {
                title =  cursor.getString(cursor.getColumnIndex(Note.TITLE_INDEX_NAME));
            }

            if (cursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME) > -1) {
                snippet =  cursor.getString(cursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME));
            }

            // Populate fields with extracted properties
            titleTextView.setText(title);
            contentTextView.setText(snippet);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get the selected note
                    Note note = mNotesAdapter.getItem((int)view.getTag());

                    // Store link between note and widget in SharedPreferences
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    preferences.edit().putString(PrefUtils.PREF_NOTE_WIDGET_NOTE + mAppWidgetId, note.getSimperiumKey()).apply();

                    // Prepare bundle for NoteEditorActivity
                    Bundle arguments = new Bundle();
                    arguments.putString(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
                    arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());

                    // Create intent to navigate to selected note on widget click
                    Intent intent = new Intent(context, NoteEditorActivity.class);
                    intent.putExtras(arguments);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, mAppWidgetId, intent, 0);

                    // Remove title from content
                    String title = note.getTitle();
                    String contentWithoutTitle = note.getContent().replace(title, "");
                    int indexOfNewline = contentWithoutTitle.indexOf("\n") + 1;
                    String content = contentWithoutTitle.substring(indexOfNewline < contentWithoutTitle.length() ? indexOfNewline : 0);

                    // Set widget content
                    mRemoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                    mRemoteViews.setTextViewText(R.id.widget_text, title);
                    mRemoteViews.setTextColor(R.id.widget_text, getResources().getColor(R.color.text_title_light, context.getTheme()));
                    mRemoteViews.setTextViewText(R.id.widget_text_title, title);
                    mRemoteViews.setTextColor(R.id.widget_text_title, context.getResources().getColor(R.color.text_title_light, context.getTheme()));
                    mRemoteViews.setTextViewText(R.id.widget_text_content, content);
                    mWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews);

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
