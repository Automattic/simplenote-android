package com.automattic.simplenote.widget;

import static com.automattic.simplenote.widget.commands.WidgetConstants.*;
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
import com.automattic.simplenote.widget.commands.NewNoteCommand;
import com.automattic.simplenote.widget.commands.NoteSelectedCommand;
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

        mCommandSet.put(ACTION_NOTIFY_DATA_SET_CHANGED,
                new NotifyDataSetChange(ListWidgetProvider.class, R.id.lv_widget_notes));
        mCommandSet.put(ACTION_NEW_NOTE, new NewNoteCommand());
        mCommandSet.put(ACTION_SEARCH_NOTE, new UnimplementedCommand());
        mCommandSet.put(ACTION_LAUNCH_APP, new UnimplementedCommand());
        mCommandSet.put(ACTION_NOTE_SELECTED, new NoteSelectedCommand());

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

            setupPendingIntents(context, appWidgetManager, appWidgetIds[i]);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rViews);

        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    /**
     * Register pending intents to widget UI buttons.
     *
     * @param ctx              context needed to access remote view.
     * @param appWidgetManager widget manager that will be updated
     * @param widgetId         widget id managed by widget manager.
     */
    private void setupPendingIntents(Context ctx, AppWidgetManager appWidgetManager, int widgetId) {

        Log.i(TAG, "setting up pending intents for widget: " + widgetId);
        PendingIntentBuilder piBuilder = new PendingIntentBuilder(ctx, appWidgetManager);
        piBuilder.setLayout(R.layout.widget_list_layout);

        piBuilder.setProvider(ListWidgetProvider.class);
        piBuilder.setWidgetId(widgetId);

        piBuilder.setAction(ACTION_NEW_NOTE);
        piBuilder.setChildView(R.id.ib_widget_new);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_SEARCH_NOTE);
        piBuilder.setChildView(R.id.ib_widget_search);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_LAUNCH_APP);
        piBuilder.setChildView(R.id.ib_widget_app_icon);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_NOTE_SELECTED);
        piBuilder.setChildView(R.id.lv_widget_notes);
        piBuilder.setPendingIntentTemplate();


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
