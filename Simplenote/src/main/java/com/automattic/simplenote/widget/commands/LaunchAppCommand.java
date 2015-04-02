package com.automattic.simplenote.widget.commands;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.automattic.simplenote.ActivityCommand;

import static com.automattic.simplenote.utils.PrefUtils.PREF_ACTIVITY_COMMAND;

/**
 * Created by richard on 4/1/15.
 */
public class LaunchAppCommand extends WidgetCommand {

    /**
     * This command will be passed to the activity on startup.
     */
    private final ActivityCommand mStartupCommand;

    /**
     * @param startupCommand the command to pass to the activity when it starts. May be {@code null}.
     */
    public LaunchAppCommand(ActivityCommand startupCommand) {
        super(null, false);
        mStartupCommand = startupCommand;
    }

    public void exec(ExecParameters params) {

        if (mStartupCommand != null){
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                    params.mContext).edit();
            editor.putString(PREF_ACTIVITY_COMMAND, mStartupCommand.name());
            editor.commit();
        }

        Intent i = new Intent(params.mContext, com.automattic.simplenote.NotesActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        params.mContext.startActivity(i);
    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}
