package com.automattic.simplenote.widget.commands;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.widget.SimpleNoteWidgetProvider;

/**
 * Created by richard on 9/7/14.
 */
public class NotifyDataSetChange extends WidgetCommand {

    public NotifyDataSetChange(){
        super(SimpleNoteWidgetProvider.ACTION_NOTIFY_DATA_SET_CHANGED, false);
    }

    public void exec(ExecParameters params) {

        // update all widgets
        int ids[] = params.mWidgetManager.getAppWidgetIds(
                new ComponentName(params.mContext, SimpleNoteWidgetProvider.class));

        if (ids != null){
            for (int i : ids) {
                Log.i(TAG, "notify data set changed. widget id: " + Integer.toString(i));
                params.mWidgetManager.notifyAppWidgetViewDataChanged(i, R.id.avf_widget_populated);
            }
        }

    }

    protected RemoteViews getRemoteViews(ExecParameters params){
       return null;
    }
}
