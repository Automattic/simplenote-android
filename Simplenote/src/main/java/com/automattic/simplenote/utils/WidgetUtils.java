package com.automattic.simplenote.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.automattic.simplenote.widget.ListWidgetProvider;

/**
 * A utility class that provides some functions for home screen widgets.
 * Created by richard on 4/2/15.
 */
public final class WidgetUtils {

    public static void sendBroadcastAppWigetUpdate(Context ctx) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(ctx.getPackageName(),
                ListWidgetProvider.class.getName()));

        if (widgetIds == null || widgetIds.length == 0){
            return;
        }


        // notify widgets so they can update themselves
        Intent i = new Intent(ctx, ListWidgetProvider.class);
        i.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));

        Bundle extras = new Bundle();
        extras.putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        i.putExtras(extras);

        ctx.sendBroadcast(i);
    }
}
