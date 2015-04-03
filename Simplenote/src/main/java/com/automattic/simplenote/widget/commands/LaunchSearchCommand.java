package com.automattic.simplenote.widget.commands;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.automattic.simplenote.ActivityCommand;

import static com.automattic.simplenote.utils.PrefUtils.PREF_ACTIVITY_COMMAND;
import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_SIMPERIUM_KEY;

/**
 * Created by richard on 4/1/15.
 */
public class LaunchSearchCommand extends WidgetCommand {

    public LaunchSearchCommand() {
        super(null, false);
    }

    public void exec(ExecParameters params) {

        Intent i = new Intent(params.mContext, com.automattic.simplenote.WidgetSearch.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        params.mContext.startActivity(i);
    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}
