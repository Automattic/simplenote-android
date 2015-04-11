package com.automattic.simplenote.widget.commands;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.automattic.simplenote.ActivityCommand;

import static com.automattic.simplenote.utils.PrefUtils.PREF_ACTIVITY_COMMAND;
import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_SIMPERIUM_KEY;

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

        Intent i = new Intent(params.mContext, com.automattic.simplenote.NotesActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_FROM_BACKGROUND);
        Log.i("Activity", "flags");

        // FLAG_ACTIVITY_NEW_TASK

        if (mStartupCommand != null){
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                    params.mContext).edit();
            editor.putString(PREF_ACTIVITY_COMMAND, mStartupCommand.name());
            editor.commit();

            Bundle extras = params.mIntent.getExtras();

            if (extras.containsKey(EXTRA_SIMPERIUM_KEY)){
                i.putExtra(EXTRA_SIMPERIUM_KEY, extras.getString(EXTRA_SIMPERIUM_KEY));
            }

        }


        params.mContext.startActivity(i);
    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}
