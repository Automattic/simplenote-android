package com.automattic.simplenote.widget.commands;

import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Created by richard on 9/7/14.
 */
public class UnimplementedCommand extends WidgetCommand {

    public UnimplementedCommand() {
        super(null, false);
    }

    public void exec(ExecParameters params) {


    }

    protected RemoteViews getRemoteViews(ExecParameters params) {
        return null;
    }
}
