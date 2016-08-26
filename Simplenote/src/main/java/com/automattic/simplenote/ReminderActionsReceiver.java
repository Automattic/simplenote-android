package com.automattic.simplenote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.automattic.simplenote.utils.AlarmUtils;
import com.automattic.simplenote.utils.NotificationUtils;

import java.util.Calendar;

/**
 * Created by jegumi on 26/08/16.
 */
public class ReminderActionsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationUtils.removeNotifications(context);
		if (intent != null) {
			String key = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_ID);
			String title = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_TITLE);
			String content = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_CONTENT);

			int action = intent.getIntExtra(NotificationUtils.ARG_ACTION_KEY, 0);
			switch (action) {
			case NotificationUtils.ARG_SNOOZE_ID:
				Calendar in10Minutes = Calendar.getInstance();
				in10Minutes.add(Calendar.MINUTE, 10);
				AlarmUtils.createAlarm(context, key, title, content, in10Minutes);
				break;
			case NotificationUtils.ARG_REMOVE_ID:
				AlarmUtils.removeAlarm(context, key, title, content, false);
				break;
			default:
				AlarmUtils.removeAlarm(context, key, title, content, false);
			}
		}
	}
}
