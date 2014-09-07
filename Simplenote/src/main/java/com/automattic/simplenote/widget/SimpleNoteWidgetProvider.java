package com.automattic.simplenote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.TagsAdapter;
import com.simperium.client.Bucket;

/**
 * Created by richard on 8/30/14.
 */
public class SimpleNoteWidgetProvider extends AppWidgetProvider{

    private static final String TAG = "WidgetProvider";

    /**
     * Intent with this action is broadcast whenever the foward button is tapped.
     */
    public static final String ACTION_FORWARD = "com.automattic.simplenote.action.ACTION_WIDGET_FORWARD";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.i(TAG, "onReceive: intent " + intent.getAction().toString());

        AppWidgetManager awManager = AppWidgetManager.getInstance(context);

        if (intent.getAction().equals(ACTION_FORWARD)){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int currentNote = prefs.getInt(WidgetService.PREF_WIDGET_NOTE, WidgetService.NO_NOTE);

            if (currentNote == WidgetService.NO_NOTE || currentNote < 0){
                currentNote = 0;
            } else {
                currentNote++;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(WidgetService.PREF_WIDGET_NOTE, currentNote);
            editor.commit();

            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
                throw new IllegalArgumentException("intent has no widget id.");
            }

            awManager.notifyAppWidgetViewDataChanged(widgetId, R.id.avf_widget_populated);
            Log.i(TAG, "note set to " + currentNote + ". Updating widget id " + widgetId);
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate. Processing " + appWidgetIds.length + " widgets.");


        // create remote views for each app widget.
        for (int i = 0; i < appWidgetIds.length; i++) {

            // create intent that starts widget service
            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);

            // add the intent URI as an extra so the OS an match it with the service.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            // create a remote view, specifying the widget layout that should be used.
            RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            rViews.setRemoteAdapter(appWidgetIds[i], R.id.avf_widget_populated, intent);

            // specify the sibling to the collection view that is shown when no data is available.
            rViews.setEmptyView(appWidgetIds[i], R.id.tv_widget_empty);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rViews);
            // appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds[i], R.id.avf_widget_populated);

            setupPendingIntents(context, appWidgetManager, appWidgetIds[i]);

        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void setupPendingIntents(Context ctx, AppWidgetManager appWidgetManager, int widgetId){

        // pending intents
        PendingIntent piForward = setupForwardPendingIntent(ctx, widgetId);

        // setup pending intents for buttons
        // Create a view that will show data for this item.
        RemoteViews rViews = new RemoteViews(ctx.getPackageName(),
                R.layout.widget_layout);
        rViews.setOnClickPendingIntent(R.id.btn_widget_forward, piForward);
        appWidgetManager.updateAppWidget(widgetId, rViews);
        Log.i(TAG, "setup forward intent)");

    }

    private PendingIntent setupForwardPendingIntent(Context ctx,
                                                    int appWidgetId ){
        Intent i = new Intent(ctx, SimpleNoteWidgetProvider.class);
        i.setAction(SimpleNoteWidgetProvider.ACTION_FORWARD);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);


        PendingIntent result = PendingIntent.getBroadcast(ctx, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return result;

    }
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.i(TAG, "onDeleted");

    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "onEnabled");

    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.i(TAG, "onDisabled");

    }



}
