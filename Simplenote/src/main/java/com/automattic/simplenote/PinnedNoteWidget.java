package com.automattic.simplenote;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Debug;
import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

public class PinnedNoteWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update all widgets.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Get note id from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("com.automattic.simplenote", Context.MODE_PRIVATE);
        String key = prefs.getString(Integer.toString(appWidgetId), "");

        if (!key.isEmpty()) {
            // Get notes bucket
            Simplenote currentApp = (Simplenote) context.getApplicationContext();
            Bucket<Note> mNotesBucket = currentApp.getNotesBucket();
            try {
                // Update note
                Note updatedNote = mNotesBucket.get(key);
                // Update widget
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pinned_note_widget);
                // Set widget content.
                views.setTextViewText(R.id.appwidget_text, updatedNote.getTitle());
                // Update widget
                AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                widgetManager.updateAppWidget(appWidgetId, views);
            } catch (BucketObjectMissingException e) {
                // Note missing.

            }
        }
    }
}

