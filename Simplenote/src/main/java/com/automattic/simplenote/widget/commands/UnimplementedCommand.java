package com.automattic.simplenote.widget.commands;

import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Created by richard on 9/7/14.
 */
public class UnimplementedCommand extends WidgetCommand {

    public UnimplementedCommand(){
        super(null, false);
    }

    public void exec(ExecParameters params) {

        Toast.makeText(params.mContext, new StringBuilder().append("Command for action ")
                .append(params.mIntent.getAction())
                .append(" not implemented yet")
                .toString(), Toast.LENGTH_SHORT).show();

    }

    protected RemoteViews getRemoteViews(ExecParameters params){
       return null;
    }
}
