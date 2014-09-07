package com.automattic.simplenote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.IntentUtil;
import com.automattic.simplenote.widget.commands.NavigateNote;
import com.automattic.simplenote.widget.commands.NotifyDataSetChange;
import com.automattic.simplenote.widget.commands.UnimplementedCommand;
import com.automattic.simplenote.widget.commands.WidgetCommand;

import java.util.Hashtable;

/**
 * Created by richard on 8/30/14.
 */
public class ListWidgetProvider extends AppWidgetProvider {


    private static final String TAG = "ListWidgetProvider";
    private Hashtable<String, WidgetCommand> mCommandSet = new Hashtable<String, WidgetCommand>();

    public ListWidgetProvider() {
        super();

        mCommandSet.put(SimpleNoteWidgetProvider.ACTION_NOTIFY_DATA_SET_CHANGED,
                new NotifyDataSetChange(ListWidgetProvider.class, R.id.lv_widget_notes));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.i(TAG, "onReceive: intent " + intent.getAction().toString());
        IntentUtil.dump(intent);

        AppWidgetManager awManager = AppWidgetManager.getInstance(context);
        String action = intent.getAction();

        if (mCommandSet.containsKey(action)) {
            mCommandSet.get(action).run(context, intent);
        } else {
            new UnimplementedCommand().run(context, intent);
        }


    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate. Processing " + appWidgetIds.length + " widgets.");


        // create remote views for each app widget.
        for (int i = 0; i < appWidgetIds.length; i++) {

            Log.i(TAG, "onUpdate. Setup widget " + appWidgetIds[i]);

            // create intent that starts widget service
            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);

            // add the intent URI as an extra so the OS can match it with the service.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            // create a remote view, specifying the widget layout that should be used.
            RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget_list_layout);
            rViews.setRemoteAdapter(R.id.lv_widget_notes, intent);

            // setupPendingIntents(context, appWidgetManager, appWidgetIds[i]);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rViews);

        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
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
