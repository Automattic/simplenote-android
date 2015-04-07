package com.automattic.simplenote.widget.commands;

import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.widget.SimpleNoteWidgetProvider;
import static com.automattic.simplenote.widget.commands.WidgetConstants.ACTION_FORWARD;

/**
 * Created by richard on 9/7/14.
 */
public class NavigateNote extends WidgetCommand {

    private final boolean mForward;

    public NavigateNote(boolean forward) {
        super(ACTION_FORWARD, true);
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

    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return new RemoteViews(params.mContext.getPackageName(), R.layout.widget_button_layout);
    }
}
