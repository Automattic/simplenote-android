package com.automattic.simplenote.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.automattic.simplenote.NoteWidget;

public class WidgetUtils {
    public static void updateNoteWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, NoteWidget.class));
        Intent updateIntent = new Intent();
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(NoteWidget.KEY_WIDGET_IDS, ids);
        context.sendBroadcast(updateIntent);
    }
}
