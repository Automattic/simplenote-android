package com.automattic.simplenote.widget.commands;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Created by richard on 9/7/14.
 */
public abstract class WidgetCommand {

    public static final String TAG = "WidgetCommand";

    public final String action;
    public final boolean widgetIdRequired;

    public WidgetCommand(String action, boolean widgetIdRequired) {
        this.action = action;
        this.widgetIdRequired = widgetIdRequired;
    }

    public final void run(Context ctx, Intent intent) {
        ExecParameters param = new ExecParameters();
        param.mContext = ctx;
        param.mIntent = intent;
        param.mWidgetManager = AppWidgetManager.getInstance(ctx);
        param.mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (widgetIdRequired && param.mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            throw new IllegalArgumentException("intent has no widget id.");
        }

        exec(param);

    }

    public abstract void exec(ExecParameters params);

    protected abstract RemoteViews getRemoteViews(ExecParameters params);

    public static class ExecParameters {
        public AppWidgetManager mWidgetManager;
        public int mWidgetId;
        public Context mContext;
        public Intent mIntent;
    }
}
