package com.automattic.simplenote.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.automattic.simplenote.models.Reminder;

public class ReminderReceiver extends BroadcastReceiver
{

  @Override
  public void onReceive(Context context, Intent intent)
  {
    Intent newIntent = new Intent(context, ReminderAlarmNotification.class);
    Reminder reminder = new Reminder("");

    reminder.fromIntent(intent);
    reminder.toIntent(newIntent);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);


    context.startActivity(newIntent);
  }
}

