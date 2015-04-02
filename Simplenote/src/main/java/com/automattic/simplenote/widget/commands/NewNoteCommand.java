package com.automattic.simplenote.widget.commands;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_ACTIVITY_COMMAND;
import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_LIST_POSITION;

/**
 * Created by richard on 9/7/14.
 */
public class NewNoteCommand extends WidgetCommand {

    public NewNoteCommand() {
        super(null, false);
    }

    public void exec(ExecParameters params) {
        Intent i = new Intent(params.mContext, com.automattic.simplenote.NotesActivity.class);
        i.putExtra(EXTRA_ACTIVITY_COMMAND, WidgetConstants.ActivityCommand.NEW_NOTE);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        params.mContext.startActivity(i);
    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}
