package com.automattic.simplenote.widget.commands;

import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_LIST_POSITION;
import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_SIMPERIUM_KEY;

/**
 * Created by richard on 9/7/14.
 */
public class NoteSelectedCommand extends WidgetCommand {

    public NoteSelectedCommand() {
        super(null, false);
    }

    public void exec(ExecParameters params) {
        Bundle b = params.mIntent.getExtras();
        int position = b.getInt(EXTRA_LIST_POSITION, Integer.MIN_VALUE);
        Toast.makeText(params.mContext, new StringBuilder()
                .append("note in position " + position + " selected: ")
                .append(b.getString(EXTRA_SIMPERIUM_KEY)), Toast.LENGTH_SHORT).show();

    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}
