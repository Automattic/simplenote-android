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
public class SimpleNoteWidgetProvider extends AppWidgetProvider {

    /**
     * Intent with this action is broadcast whenever the foward button is tapped.
     */
    public static final String ACTION_FORWARD =
            "com.automattic.simplenote.action.ACTION_WIDGET_FORWARD";
    public static final String ACTION_BACKWARD =
            "com.automattic.simplenote.action.ACTION_WIDGET_BACKWARD";
    public static final String ACTION_DELETE_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_DELETE";
    public static final String ACTION_NEW_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_NEW_NOTE";
    public static final String ACTION_SEARCH_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_SEARCH";
    public static final String ACTION_SHARE_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_SHARE";
    public static final String ACTION_SHOW_ALL_NOTES =
            "com.automattic.simplenote.action.ACTION_WIDGET_SHOW_ALL";
    public static final String ACTION_LAUNCH_APP =
            "com.automattic.simplenote.action.ACTION_WIDGET_LAUNCH_APP";
    public static final String ACTION_NOTIFY_DATA_SET_CHANGED =
            "com.automattic.simplenote.action.ACTION_NOTIFY_DATA_SET_CHANGED";

    private static final String TAG = "WidgetProvider";
    private Hashtable<String, WidgetCommand> mCommandSet = new Hashtable<String, WidgetCommand>();

    public SimpleNoteWidgetProvider() {
        super();

        mCommandSet.put(ACTION_FORWARD, new NavigateNote(true)); // nav to next note
        mCommandSet.put(ACTION_BACKWARD, new NavigateNote(false)); // nav to previous note
        mCommandSet.put(ACTION_NOTIFY_DATA_SET_CHANGED, new NotifyDataSetChange());
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
            RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
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

        Log.i(TAG, "setting up pending intents for widget: " + widgetId);
        PendingIntentBuilder piBuilder = new PendingIntentBuilder(ctx, appWidgetManager);
        piBuilder.setLayout(R.layout.widget_layout);
        piBuilder.setWidgetId(widgetId);

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_BACKWARD);
        piBuilder.setChildView(R.id.ib_widget_backward);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_FORWARD);
        piBuilder.setChildView(R.id.ib_widget_forward);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_DELETE_NOTE);
        piBuilder.setChildView(R.id.ib_widget_delete);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_NEW_NOTE);
        piBuilder.setChildView(R.id.ib_widget_new);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_SEARCH_NOTE);
        piBuilder.setChildView(R.id.ib_widget_search);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_SHARE_NOTE);
        piBuilder.setChildView(R.id.ib_widget_share);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_SHOW_ALL_NOTES);
        piBuilder.setChildView(R.id.ib_widget_showallnotes);
        piBuilder.setOnClickPendingIntent();

        piBuilder.setAction(SimpleNoteWidgetProvider.ACTION_LAUNCH_APP);
        piBuilder.setChildView(R.id.ib_widget_app_icon);
        piBuilder.setOnClickPendingIntent();

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


    private static class PendingIntentBuilder {

        private final Context mContext;
        private final AppWidgetManager mManager;
        private Integer mLayoutResId;
        private Integer mChildViewResId;
        private Integer mWidgetId;
        private String mAction;

        public PendingIntentBuilder(Context ctx, AppWidgetManager manager) {
            mContext = ctx;
            mManager = manager;

        }

        public PendingIntent build() {

            validate(mLayoutResId, "layout resource id", "setLayout(int)");
            validate(mChildViewResId, "child resource id", "setChildView(int)");
            validate(mAction, "action", "setAction(String)");
            validate(mWidgetId, "widget id", "setWidgetId(int)");

            Intent i = new Intent(mContext, SimpleNoteWidgetProvider.class);
            i.setAction(mAction);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);

            IntentUtil.dump(i);

            /*
             * Using the widget id as the request code makes the PendingIntent unique
             * for a given action and widget id.  But, widget id still needs to be
             * added as an extra (EXTRA_APPWIDGET_ID) because the request code is
             * not sent with the intent that is broadcast.
             */
            return PendingIntent.getBroadcast(mContext, mWidgetId, i,
                    PendingIntent.FLAG_UPDATE_CURRENT);

        }

        private void validate(Object underTest, String name, String setterName) {
            if (underTest == null) {
                throw new IllegalStateException(new StringBuilder().append(name)
                        .append(" cannot be null. Call ")
                        .append(setterName)
                        .toString());
            }
        }

        public PendingIntentBuilder setLayout(int resId) {
            mLayoutResId = resId;
            return this;
        }

        public PendingIntentBuilder setChildView(int resId) {
            mChildViewResId = resId;
            return this;
        }

        public PendingIntentBuilder setAction(String action) {
            mAction = action;
            return this;
        }

        public PendingIntentBuilder setWidgetId(int widgetId) {
            mWidgetId = widgetId;
            return this;
        }

        public void setPendingIntentTemplate() {
            // setup pending intents for buttons
            // Create a view that will show data for this item.
            RemoteViews rViews = new RemoteViews(mContext.getPackageName(),
                    mLayoutResId);
            rViews.setPendingIntentTemplate(mChildViewResId, build());
            Log.i(TAG, "setPendingIntentTemplate set for remote view with action " + mAction);
        }

        public void setOnClickPendingIntent() {


            // setup pending intents for buttons
            // Create a view that will show data for this item.
            RemoteViews rViews = new RemoteViews(mContext.getPackageName(),
                    mLayoutResId);
            rViews.setOnClickPendingIntent(mChildViewResId, build());
            mManager.updateAppWidget(mWidgetId, rViews);

            Log.i(TAG, "onClickPendingIntent set for remote view with action " + mAction);

        }

        /**
         * Clears out everything set through builder functions.  The values passed to the
         * constructor are untouched.
         */
        public void clear() {
            mLayoutResId = null;
            mChildViewResId = null;
            mAction = null;

        }


    }
}
