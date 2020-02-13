package com.automattic.simplenote.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.automattic.simplenote.NoteWidgetLight;

public class WidgetUtils {
    public static void updateNoteWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] idsLight = appWidgetManager.getAppWidgetIds(new ComponentName(context, NoteWidgetLight.class));
        Intent updateIntentLight = new Intent();
        updateIntentLight.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntentLight.putExtra(NoteWidgetLight.KEY_WIDGET_IDS_LIGHT, idsLight);
        context.sendBroadcast(updateIntentLight);
    }
}
