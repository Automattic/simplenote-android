package com.automattic.simplenote.utils;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.automattic.simplenote.ReminderReceiver;

import java.util.Calendar;
/**
 * Created by Day7 on 30.09.16.
 */

public class AlarmUtils {

    public static final String REMINDER_EXTRA_ID = "reminderId";
    public static final String REMINDER_EXTRA_TITLE = "reminderTitle";
    public static final String REMINDER_EXTRA_CONTENT = "reminderContent";

    public static void createAlarm(Context context, String id, String title, String content, Calendar calendar) {
        //enableAlarmReceiver(context);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(REMINDER_EXTRA_ID, id);
        intent.putExtra(REMINDER_EXTRA_TITLE, title);
        intent.putExtra(REMINDER_EXTRA_CONTENT, content);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        calendar.set(Calendar.SECOND, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    public static void removeAlarm(Context context, String id, String title, String content) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(REMINDER_EXTRA_ID, id);
        intent.putExtra(REMINDER_EXTRA_TITLE, title);
        intent.putExtra(REMINDER_EXTRA_CONTENT, content);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    private static void enableAlarmReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, ReminderReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private static void disableAlarmReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, ReminderReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
