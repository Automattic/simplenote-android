package com.automattic.simplenote;

import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_WIDGET;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_NOTE_NOT_FOUND_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_NOTE_TAPPED;
import static com.automattic.simplenote.utils.WidgetUtils.KEY_WIDGET_CLICK;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
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
import com.automattic.simplenote.utils.ChecklistUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.Query;
import com.simperium.client.User;

public class NoteWidgetLightConfigureActivity extends AppCompatActivity {
    private AppWidgetManager mWidgetManager;
    private NotesCursorAdapter mNotesAdapter;
    private RemoteViews mRemoteViews;
    private Simplenote mApplication;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public NoteWidgetLightConfigureActivity() {
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
            Toast.makeText(NoteWidgetLightConfigureActivity.this, R.string.log_in_add_widget, Toast.LENGTH_LONG).show();
            finish();
        }

        // Get widget information
        mWidgetManager = AppWidgetManager.getInstance(NoteWidgetLightConfigureActivity.this);
        mRemoteViews = new RemoteViews(getPackageName(), PrefUtils.getLayoutWidget(NoteWidgetLightConfigureActivity.this, true));
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
        PrefUtils.sortNoteQuery(query, NoteWidgetLightConfigureActivity.this, true);
        ObjectCursor<Note> cursor = query.execute();

        Context context = new ContextThemeWrapper(NoteWidgetLightConfigureActivity.this, PrefUtils.getStyleWidgetDialog(NoteWidgetLightConfigureActivity.this));
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams")
        final View layout = LayoutInflater.from(context).inflate(R.layout.note_widget_configure_list, null);
        final ListView list = layout.findViewById(R.id.list);
        mNotesAdapter = new NotesCursorAdapter(NoteWidgetLightConfigureActivity.this, cursor);
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
            return LayoutInflater.from(context).inflate(PrefUtils.getLayoutWidgetListItem(context, ThemeUtils.isLightTheme(context)), parent, false);
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

            if (cursor.getColumnIndexOrThrow(Note.TITLE_INDEX_NAME) > -1) {
                title =  cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE_INDEX_NAME));
            }

            if (cursor.getColumnIndexOrThrow(Note.CONTENT_PREVIEW_INDEX_NAME) > -1) {
                snippet =  cursor.getString(cursor.getColumnIndexOrThrow(Note.CONTENT_PREVIEW_INDEX_NAME));
            }

            // Populate fields with extracted properties
            titleTextView.setText(title);
            SpannableStringBuilder snippetSpan = new SpannableStringBuilder(snippet);
            snippetSpan = (SpannableStringBuilder) ChecklistUtils.addChecklistSpansForRegexAndColor(
                context,
                snippetSpan,
                ChecklistUtils.CHECKLIST_REGEX,
                R.color.text_title_disabled,
                true
            );
            contentTextView.setText(snippetSpan);

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
                    arguments.putBoolean(NoteEditorFragment.ARG_IS_FROM_WIDGET, true);
                    arguments.putString(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
                    arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());
                    arguments.putBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED, note.isPreviewEnabled());

                    // Create intent to navigate to selected note on widget click
                    Intent intent = new Intent(context, NoteEditorActivity.class);
                    intent.putExtras(arguments);
                    intent.putExtra(KEY_WIDGET_CLICK, NOTE_WIDGET_NOTE_TAPPED);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, mAppWidgetId, intent, PendingIntent.FLAG_IMMUTABLE);

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
                    SpannableStringBuilder contentSpan = new SpannableStringBuilder(content);
                    contentSpan = (SpannableStringBuilder) ChecklistUtils.addChecklistUnicodeSpansForRegex(
                            contentSpan,
                            ChecklistUtils.CHECKLIST_REGEX
                    );
                    mRemoteViews.setTextViewText(R.id.widget_text_content, contentSpan);
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
