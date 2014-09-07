package com.automattic.simplenote.widget.commands;

import android.content.ComponentName;
import android.util.Log;
import android.widget.RemoteViews;

import com.automattic.simplenote.R;
import com.automattic.simplenote.widget.SimpleNoteWidgetProvider;

/**
 * Created by richard on 9/7/14.
 */
public class NotifyDataSetChange extends WidgetCommand {

    private final Class mProviderClass;
    private final int mAdapterRes;

    public NotifyDataSetChange(Class providerClass, int adapterRes) {
        super(SimpleNoteWidgetProvider.ACTION_NOTIFY_DATA_SET_CHANGED, false);
        mProviderClass = providerClass;
        mAdapterRes = adapterRes;
    }

    public void exec(ExecParameters params) {

        // update all widgets
        int ids[] = params.mWidgetManager.getAppWidgetIds(
                new ComponentName(params.mContext, mProviderClass));

        Log.i(TAG, "provider " + mProviderClass.getSimpleName());

        if (ids != null && ids.length > 0) {
            for (int i : ids) {
                Log.i(TAG, "Notify data set changed. widget id: " + Integer.toString(i));
                params.mWidgetManager.notifyAppWidgetViewDataChanged(i, mAdapterRes);
            }
        } else {
            Log.i(TAG, "no widgets were found for provider " +
                    mProviderClass.getSimpleName());
        }

    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}
