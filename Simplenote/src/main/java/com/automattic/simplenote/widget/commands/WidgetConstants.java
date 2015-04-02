package com.automattic.simplenote.widget.commands;

/**
 * Created by richard on 3/31/15.
 */
public class WidgetConstants {

    /**
     * Intent with this action is broadcast whenever the forward button is tapped.
     */
    public static final String ACTION_FORWARD =
            "com.automattic.simplenote.action.ACTION_WIDGET_FORWARD";
    public static final String ACTION_BACKWARD =
            "com.automattic.simplenote.action.ACTION_WIDGET_BACKWARD";
    public static final String ACTION_DELETE_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_DELETE";
    public static final String ACTION_NEW_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_NEW_NOTE";
    public static final String ACTION_SEARCH_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_SEARCH";
    public static final String ACTION_SHARE_NOTE =
            "com.automattic.simplenote.action.ACTION_WIDGET_SHARE";
    public static final String ACTION_SHOW_ALL_NOTES =
            "com.automattic.simplenote.action.ACTION_WIDGET_SHOW_ALL";
    public static final String ACTION_LAUNCH_APP =
            "com.automattic.simplenote.action.ACTION_WIDGET_LAUNCH_APP";
    public static final String ACTION_NOTIFY_DATA_SET_CHANGED =
            "com.automattic.simplenote.action.ACTION_NOTIFY_DATA_SET_CHANGED";
    public static final String ACTION_NOTE_SELECTED =
            "com.automattic.simplenote.action.ACTION_NOTE_SELECTED";

    public static final String EXTRA_LIST_POSITION = "EXTRA_LIST_POSITION";

    public static final String EXTRA_ACTIVITY_COMMAND = "EXTRA_ACTIVITY_COMMAND";

    public enum ActivityCommand{
        NEW_NOTE,
        EDIT_NOTE
    }
}
