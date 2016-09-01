package com.automattic.simplenote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.automattic.simplenote.utils.AlarmUtils;
import com.automattic.simplenote.utils.NotificationUtils;

import java.util.Calendar;

/**
 * Created by jegumi on 25/08/16.
 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AlarmUtils", "Time in millis: " + Calendar.getInstance().getTimeInMillis());
        String id = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_ID);
        String title = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_TITLE);
        String content = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_CONTENT);
        NotificationUtils.showAlarmNotification(context, id, title, content);
    }
}
