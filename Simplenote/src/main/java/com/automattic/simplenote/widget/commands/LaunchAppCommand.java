package com.automattic.simplenote.widget.commands;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_ACTIVITY_COMMAND;
import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_LIST_POSITION;

/**
 * Created by richard on 4/1/15.
 */
public class LaunchAppCommand extends WidgetCommand {

    /**
     * This command will be passed to the activity on startup.
     */
    private final WidgetConstants.ActivityCommand mStartupCommand;

    /**
     * @param startupCommand the command to pass to the activity when it starts. May be {@code null}.
     */
    public LaunchAppCommand(WidgetConstants.ActivityCommand startupCommand) {
        super(null, false);
        mStartupCommand = startupCommand;
    }

    public void exec(ExecParameters params) {

        if (mStartupCommand != null){
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                    params.mContext).edit();
            editor.putString(EXTRA_ACTIVITY_COMMAND, mStartupCommand.name());
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
