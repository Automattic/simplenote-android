package com.automattic.simplenote.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.automattic.simplenote.ReminderReceiver;

import java.util.Calendar;

/**
 * Created by jegumi on 25/08/16.
 */
public class AlarmUtils {

	public static final String REMINDER_EXTRA_ID = "reminderId";
	public static final String REMINDER_EXTRA_TITLE = "reminderTitle";
	public static final String REMINDER_EXTRA_CONTENT = "reminderContent";

	public static void createAlarm(Context context, String id, String title, String content, Calendar calendar) {
		// enableAlarmReceiver(context);

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

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pendingIntent);
	}

	public static void removeAlarm(Context context, String id, String title, String content, boolean isRepeated) {
		NoteUtils.removeNoteReminder(id);
		if (isRepeated) {
			removeAlarm(context, id, title, content);
		}
	}
}
