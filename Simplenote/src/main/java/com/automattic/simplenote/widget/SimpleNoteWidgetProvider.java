package com.automattic.simplenote.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.widget.commands.NotifyDataSetChange;
import com.automattic.simplenote.widget.commands.UnimplementedCommand;
import com.automattic.simplenote.widget.commands.WidgetCommand;

import java.util.Hashtable;
import static com.automattic.simplenote.widget.commands.WidgetConstants.*;


// TODO: This class and ListWidgetProvider have some common code. Refactor it into a base class.

/**
 * Created by richard on 8/30/14.
 */
public class SimpleNoteWidgetProvider extends AppWidgetProvider {



    private static final String TAG = "ButtonWidgetProvider";
    private Hashtable<String, WidgetCommand> mCommandSet = new Hashtable<String, WidgetCommand>();

    public SimpleNoteWidgetProvider() {
        super();

        mCommandSet.put(ACTION_NOTIFY_DATA_SET_CHANGED,
                new NotifyDataSetChange(SimpleNoteWidgetProvider.class, R.id.avf_widget_populated));
        mCommandSet.put(ACTION_SEARCH_NOTE, new UnimplementedCommand());
        mCommandSet.put(ACTION_DELETE_NOTE, new UnimplementedCommand());
        mCommandSet.put(ACTION_LAUNCH_APP, new UnimplementedCommand());
        mCommandSet.put(ACTION_NEW_NOTE, new UnimplementedCommand());
        mCommandSet.put(ACTION_SHARE_NOTE, new UnimplementedCommand());
        mCommandSet.put(ACTION_SHOW_ALL_NOTES, new UnimplementedCommand());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager awManager = AppWidgetManager.getInstance(context);
        String action = intent.getAction();

        if (mCommandSet.containsKey(action)) {
            mCommandSet.get(action).run(context, intent);
        }

    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // create remote views for each app widget.
        for (int i = 0; i < appWidgetIds.length; i++) {

            // create intent that starts widget service
            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);

            // add the intent URI as an extra so the OS can match it with the service.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            // create a remote view, specifying the widget layout that should be used.
            RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget_button_layout);
            rViews.setRemoteAdapter(R.id.avf_widget_populated, intent);

            // specify the sibling to the collection view that is shown when no data is available.
            rViews.setEmptyView(R.id.tv_widget_empty, R.id.tv_widget_empty);

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

        PendingIntentBuilder piBuilder = new PendingIntentBuilder(ctx, appWidgetManager);
        piBuilder.setProvider(SimpleNoteWidgetProvider.class);
        piBuilder.setLayout(R.layout.widget_button_layout);
        piBuilder.setWidgetId(widgetId);

        piBuilder.setAction(ACTION_DELETE_NOTE);
        piBuilder.setChildView(R.id.ib_widget_delete);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_NEW_NOTE);
        piBuilder.setChildView(R.id.ib_widget_new);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_SEARCH_NOTE);
        piBuilder.setChildView(R.id.ib_widget_search);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_SHARE_NOTE);
        piBuilder.setChildView(R.id.ib_widget_share);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_SHOW_ALL_NOTES);
        piBuilder.setChildView(R.id.ib_widget_showallnotes);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(ACTION_LAUNCH_APP);
        piBuilder.setChildView(R.id.ib_widget_app_icon);
        piBuilder.setOnClickPendingIntent();

    }


}
