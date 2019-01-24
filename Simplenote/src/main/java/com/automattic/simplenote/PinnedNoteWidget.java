package com.automattic.simplenote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
        // Get widget views
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pinned_note_widget);
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

                // Prepare bundle for NoteEditorActivity
                Bundle arguments = new Bundle();
                arguments.putString(NoteEditorFragment.ARG_ITEM_ID, updatedNote.getSimperiumKey());
                arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, updatedNote.isMarkdownEnabled());

                // Create intent to navigate to selected note on widget click
                Intent intent = new Intent(context, NoteEditorActivity.class);
                intent.putExtras(arguments);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);

                // Set widget content
                views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                views.setTextViewText(R.id.widget_text, updatedNote.getTitle());
                views.setImageViewResource(R.id.widget_logo, R.drawable.simplenote_logo);
                appWidgetManager.updateAppWidget(appWidgetId, views);

            } catch (BucketObjectMissingException e) {
                // Note missing.
                views.setTextViewText(R.id.widget_text, "Note Not Found");
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        } else
        {
            views.setTextViewText(R.id.widget_text, "Note Not Found");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}

