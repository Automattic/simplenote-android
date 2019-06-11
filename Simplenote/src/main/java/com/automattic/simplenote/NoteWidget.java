package com.automattic.simplenote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.User;

import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_WIDGET;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_DELETED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_FIRST_ADDED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_LAST_DELETED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_NOTE_NOT_FOUND_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_NOTE_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_SIGN_IN_TAPPED;

public class NoteWidget extends AppWidgetProvider {
    public static final String KEY_WIDGET_CLICK = "key_widget_click";
    public static final String KEY_WIDGET_IDS = "key_widget_ids";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null && intent.hasExtra(KEY_WIDGET_IDS)) {
            int[] ids = intent.getExtras().getIntArray(KEY_WIDGET_IDS);
            this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        AnalyticsTracker.track(
            NOTE_WIDGET_DELETED,
            CATEGORY_WIDGET,
            "note_widget_deleted"
        );
    }

    @Override
    public void onEnabled(Context context) {
        AnalyticsTracker.track(
            NOTE_WIDGET_FIRST_ADDED,
            CATEGORY_WIDGET,
            "note_widget_first_added"
        );
    }

    @Override
    public void onDisabled(Context context) {
        AnalyticsTracker.track(
            NOTE_WIDGET_LAST_DELETED,
            CATEGORY_WIDGET,
            "note_widget_last_deleted"
        );
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Get widget views
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_widget);

        // Verify user authentication.
        Simplenote currentApp = (Simplenote) context.getApplicationContext();
        Simperium simperium = currentApp.getSimperium();
        User user = simperium.getUser();

        if (user.getStatus().equals(User.Status.NOT_AUTHORIZED)) {
            // Create intent to navigate to notes activity which redirects to signin on widget click
            Intent intent = new Intent(context, NotesActivity.class);
            intent.putExtra(KEY_WIDGET_CLICK, NOTE_WIDGET_SIGN_IN_TAPPED);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);

            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            views.setTextViewText(R.id.widget_text, context.getResources().getString(R.string.sign_in_use_widget));
            views.setTextColor(R.id.widget_text, context.getResources().getColor(R.color.gray_light));
        } else {
            // Get note id from SharedPreferences
            String key =  PrefUtils.getStringPref(context, PrefUtils.PREF_NOTE_WIDGET_NOTE + appWidgetId);

            if (!key.isEmpty()) {
                // Get notes bucket
                Bucket<Note> notesBucket = currentApp.getNotesBucket();

                try {
                    // Update note
                    Note updatedNote = notesBucket.get(key);

                    // Prepare bundle for NoteEditorActivity
                    Bundle arguments = new Bundle();
                    arguments.putString(NoteEditorFragment.ARG_ITEM_ID, updatedNote.getSimperiumKey());
                    arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, updatedNote.isMarkdownEnabled());

                    // Create intent to navigate to selected note on widget click
                    Intent intent = new Intent(context, NoteEditorActivity.class);
                    intent.putExtras(arguments);
                    intent.putExtra(KEY_WIDGET_CLICK, NOTE_WIDGET_NOTE_TAPPED);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // Set widget content
                    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                    views.setTextViewText(R.id.widget_text, updatedNote.getTitle());
                    views.setTextColor(R.id.widget_text, context.getResources().getColor(R.color.gray_dark));
                } catch (BucketObjectMissingException e) {
                    // Create intent to navigate to widget configure activity on widget click
                    Intent intent = new Intent(context, NoteWidgetConfigureActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(KEY_WIDGET_CLICK, NOTE_WIDGET_NOTE_NOT_FOUND_TAPPED);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                    views.setTextViewText(R.id.widget_text, context.getResources().getString(R.string.note_not_found));
                    views.setTextColor(R.id.widget_text, context.getResources().getColor(R.color.gray_light));
                }
            } else {
                views.setOnClickPendingIntent(R.id.widget_layout, null);
                views.setTextViewText(R.id.widget_text, context.getResources().getString(R.string.note_not_found));
                views.setTextColor(R.id.widget_text, context.getResources().getColor(R.color.gray_light));
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
