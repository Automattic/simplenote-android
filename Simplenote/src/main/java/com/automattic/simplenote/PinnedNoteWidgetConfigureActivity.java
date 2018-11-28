package com.automattic.simplenote;

import android.app.Activity;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;

public class PinnedNoteWidgetConfigureActivity extends Activity {

    private Bucket<Note> mNotesBucket;
    private NotesCursorAdapter mCursorAdapter;

    private static final String PREFS_NAME = "com.automattic.simplenote.PinnedNoteWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText mAppWidgetText;
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = PinnedNoteWidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
            String widgetText = mAppWidgetText.getText().toString();
            saveTitlePref(context, mAppWidgetId, widgetText);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            PinnedNoteWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    public PinnedNoteWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.pinned_note_widget_configure);

        /*mAppWidgetText = (EditText) findViewById(R.id.appwidget_text);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mAppWidgetText.setText(loadTitlePref(PinnedNoteWidgetConfigureActivity.this, mAppWidgetId));*/

        Simplenote currentApp = (Simplenote) getApplication();
        if (mNotesBucket == null) {
            mNotesBucket = currentApp.getNotesBucket();
        }
        Cursor cursor = mNotesBucket.query().execute();

        // Debug cursor info.
        if (cursor.moveToFirst()) {
            do {
                StringBuilder sb = new StringBuilder();
                int columnsQty = cursor.getColumnCount();
                for (int idx=0; idx<columnsQty; ++idx) {
                    sb.append(cursor.getString(idx));
                    if (idx < columnsQty - 1)
                        sb.append("; ");
                }
                Log.v("HOLA:", String.format("Row: %d, Values: %s", cursor.getPosition(),
                        sb.toString()));
            } while (cursor.moveToNext());
        }

        mCursorAdapter = new NotesCursorAdapter(this, cursor);
        ListView lv = (ListView)findViewById(R.id.lista);
        lv.setAdapter(mCursorAdapter);
    }

    public class NotesCursorAdapter extends CursorAdapter {
        public NotesCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.note_list_row, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView tvBody = (TextView) view.findViewById(R.id.note_title);
            // Extract properties from cursor
            String title = "Hola";
            if(cursor.getColumnIndex(Note.TITLE_INDEX_NAME) > -1) title = cursor.getString(cursor.getColumnIndex(Note.TITLE_INDEX_NAME));
            // Populate fields with extracted properties
            tvBody.setText(title);
        }
    }
}

