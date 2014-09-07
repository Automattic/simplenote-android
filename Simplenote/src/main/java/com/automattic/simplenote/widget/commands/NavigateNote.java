package com.automattic.simplenote.widget.commands;

import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.widget.SimpleNoteWidgetProvider;

/**
 * Created by richard on 9/7/14.
 */
public class NavigateNote extends WidgetCommand {

    private final boolean mForward;

    public NavigateNote(boolean forward){
        super(SimpleNoteWidgetProvider.ACTION_FORWARD, true);
        mForward = forward;
    }

    public void exec(ExecParameters params) {

        RemoteViews rViews = getRemoteViews(params);
        if (mForward) {
            rViews.showNext(R.id.avf_widget_populated);
        } else {
            rViews.showPrevious(R.id.avf_widget_populated);
        }

        params.mWidgetManager.updateAppWidget(params.mWidgetId, rViews);

        Log.i(TAG, "show next note for widget id " + params.mWidgetId);
    }

    protected RemoteViews getRemoteViews(ExecParameters params){
       return new RemoteViews(params.mContext.getPackageName(), R.layout.widget_layout);
    }
}
