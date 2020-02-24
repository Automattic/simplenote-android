package com.automattic.simplenote.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.automattic.simplenote.NoteListWidgetDark;
import com.automattic.simplenote.NoteListWidgetLight;
import com.automattic.simplenote.NoteWidgetDark;
import com.automattic.simplenote.NoteWidgetLight;

public class WidgetUtils {
    public static final String KEY_LIST_WIDGET_CLICK = "key_list_widget_click";
    public static final String KEY_WIDGET_CLICK = "key_widget_click";
    public static final int MINIMUM_HEIGHT_FOR_BUTTON = 150;
    public static final int MINIMUM_WIDTH_FOR_BUTTON = 300;

    public static void updateNoteWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] idsDark = appWidgetManager.getAppWidgetIds(new ComponentName(context, NoteWidgetDark.class));
        Intent updateIntentDark = new Intent();
        updateIntentDark.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntentDark.putExtra(NoteWidgetDark.KEY_WIDGET_IDS_DARK, idsDark);
        context.sendBroadcast(updateIntentDark);

        int[] idsLight = appWidgetManager.getAppWidgetIds(new ComponentName(context, NoteWidgetLight.class));
        Intent updateIntentLight = new Intent();
        updateIntentLight.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntentLight.putExtra(NoteWidgetLight.KEY_WIDGET_IDS_LIGHT, idsLight);
        context.sendBroadcast(updateIntentLight);

        int[] idsListDark = appWidgetManager.getAppWidgetIds(new ComponentName(context, NoteListWidgetDark.class));
        Intent updateIntentListDark = new Intent();
        updateIntentListDark.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntentListDark.putExtra(NoteListWidgetDark.KEY_LIST_WIDGET_IDS_DARK, idsListDark);
        context.sendBroadcast(updateIntentListDark);

        int[] idsListLight = appWidgetManager.getAppWidgetIds(new ComponentName(context, NoteListWidgetLight.class));
        Intent updateIntentListLight = new Intent();
        updateIntentListLight.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntentListLight.putExtra(NoteListWidgetLight.KEY_LIST_WIDGET_IDS_LIGHT, idsListLight);
        context.sendBroadcast(updateIntentListLight);
    }
}
