package com.automattic.simplenote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.IntentUtil;

/**
 * A class used to build pending intents for home screen widgets.
 *
 * Created by richard on 3/31/15.
 */
/* default */ final class PendingIntentBuilder {

    private final Context mContext;
    private final AppWidgetManager mManager;
    private Integer mLayoutResId;
    private Integer mChildViewResId;
    private Integer mWidgetId;
    private String mAction;
    private Class<?> mProvider;


    public PendingIntentBuilder(Context ctx, AppWidgetManager manager) {
        mContext = ctx;
        mManager = manager;

    }

    public PendingIntent build() {

        validate(mLayoutResId, "layout resource id", "setLayout(int)");
        validate(mChildViewResId, "child resource id", "setChildView(int)");
        validate(mAction, "action", "setAction(String)");
        validate(mWidgetId, "widget id", "setWidgetId(int)");
        validate(mProvider, "provider", "setProvider(Class<?>)");

        Intent i = new Intent(mContext, mProvider);
        i.setAction(mAction);
        i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);

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

    public PendingIntentBuilder setProvider(Class<?> c){
        mProvider = c;
        return this;
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

        mManager.updateAppWidget(mWidgetId, rViews);
    }

    public void setOnClickPendingIntent() {


        // setup pending intents for buttons
        // Create a view that will show data for this item.
        RemoteViews rViews = new RemoteViews(mContext.getPackageName(),
                mLayoutResId);
        rViews.setOnClickPendingIntent(mChildViewResId, build());
        mManager.updateAppWidget(mWidgetId, rViews);


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
