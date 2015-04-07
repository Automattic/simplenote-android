package com.automattic.simplenote.widget.commands;

import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_LIST_POSITION;

/**
 * Created by richard on 4/1/15.
 */
public class SearchNoteCommand extends WidgetCommand {

    public SearchNoteCommand() {
        super(null, false);
    }

    public void exec(ExecParameters params) {
        Bundle b = params.mIntent.getExtras();
        int position = b.getInt(EXTRA_LIST_POSITION, Integer.MIN_VALUE);
    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}