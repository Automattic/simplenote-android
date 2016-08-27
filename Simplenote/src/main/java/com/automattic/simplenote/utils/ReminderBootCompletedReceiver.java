package com.automattic.simplenote.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderBootCompletedReceiver extends BroadcastReceiver
{
  @Override
  public void onReceive(Context context, Intent intent)
  {
    new ReminderListAdapter(context,"");
  }
}

