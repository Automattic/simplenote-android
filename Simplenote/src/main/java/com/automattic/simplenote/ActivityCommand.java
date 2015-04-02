package com.automattic.simplenote;

/**
 * This enumeration can be set in shared preferences key
 * {@link com.automattic.simplenote.utils.PrefUtils#PREF_ACTIVITY_COMMAND}.  If it is, then the
 * specified command will be executed and removed from shared preferences so it's only executed
 * once on activity startup.
 * Created by richard on 4/1/15.
 */
public enum ActivityCommand{
    /**
     * A new note is created immediately upon startup.
     */
    NEW_NOTE,

    /**
     * A specific note is edited immediately upon startup.
     */
    EDIT_NOTE
}
