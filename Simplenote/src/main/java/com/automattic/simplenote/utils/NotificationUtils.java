package com.automattic.simplenote.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.automattic.simplenote.NoteEditorActivity;
import com.automattic.simplenote.NoteEditorFragment;
import com.automattic.simplenote.R;
import com.automattic.simplenote.ReminderActionsReceiver;

/**
 * Created by jegumi on 25/08/16.
 */
public class NotificationUtils {

    private static final int REMINDER_NOTIFICATION_ID = 1;
    public static final String ARG_ACTION_KEY = "actionId";
    public static final int ARG_OPEN_ID = 0;
    public static final int ARG_SNOOZE_ID = 1;
    public static final int ARG_REMOVE_ID = 2;
    public static final int ARG_DISMISS_ID = 3;

    public static void removeNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static void showAlarmNotification(Context context, String noteID, String title, String content) {
        Bundle arguments = new Bundle();
        arguments.putString(AlarmUtils.REMINDER_EXTRA_ID, noteID);
        arguments.putString(AlarmUtils.REMINDER_EXTRA_TITLE, title);
        arguments.putString(AlarmUtils.REMINDER_EXTRA_CONTENT, content);

        Intent openNoteIntent = getOpenIntent(context, noteID);
        Intent snoozeReminderIntent = getSnoozeIntent(context, arguments);
        Intent removeReminderIntent = getRemoveIntent(context, arguments);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(NoteEditorActivity.class);
        stackBuilder.addNextIntent(openNoteIntent);

        PendingIntent openNotePendingIntent = stackBuilder.getPendingIntent(ARG_OPEN_ID, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent snoozeReminder = PendingIntent.getBroadcast(context, ARG_SNOOZE_ID, snoozeReminderIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent removeReminder = PendingIntent.getBroadcast(context, ARG_REMOVE_ID, removeReminderIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent dismissReminder = PendingIntent.getBroadcast(context, ARG_DISMISS_ID, removeReminderIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(context.getString(R.string.reminder_title))
                        .setContentText(title)
                        .addAction(R.drawable.ic_action_alarm_snooze_24dp, context.getString(R.string.reminder_snooze), snoozeReminder)
                        .addAction(R.drawable.ic_action_alarm_off_24dp, context.getString(R.string.reminder_stop), removeReminder)
                        .setDeleteIntent(dismissReminder)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setContentIntent(openNotePendingIntent)
                        .setLights(Color.BLUE, 300, 1000)
                        .setSound(alarmSound);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(REMINDER_NOTIFICATION_ID, builder.build());
    }

    private static Intent getOpenIntent(Context context, String noteID) {
        Intent intent = new Intent(context, NoteEditorActivity.class);
        intent.putExtra(NoteEditorFragment.ARG_ITEM_ID, noteID);
        intent.putExtra(NoteEditorFragment.ARG_REMOVE_REMINDER, true);

        return intent;
    }

    private static Intent getSnoozeIntent(Context context, final Bundle arguments) {
        Intent intent = new Intent(context, ReminderActionsReceiver.class);
        arguments.putInt(ARG_ACTION_KEY, ARG_SNOOZE_ID);
        intent.putExtras(arguments);

        return intent;
    }

    private static Intent getRemoveIntent(Context context, final Bundle arguments) {
        Intent intent = new Intent(context, ReminderActionsReceiver.class);
        arguments.putInt(ARG_ACTION_KEY, ARG_REMOVE_ID);
        intent.putExtras(arguments);

        return intent;
    }
}
