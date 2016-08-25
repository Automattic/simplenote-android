package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.automattic.simplenote.utils.AlarmUtils;
import com.automattic.simplenote.utils.NotificationUtils;

import java.util.Calendar;

public class ReminderActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationUtils.removeNotifications(this);
        Intent intent = getIntent();
        if (intent != null) {
            String key = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_ID);
            String title = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_TITLE);
            String content = intent.getStringExtra(AlarmUtils.REMINDER_EXTRA_CONTENT);

            int action = intent.getIntExtra(NotificationUtils.ARG_ACTION_KEY, 0);
            switch (action) {
                case NotificationUtils.ARG_SNOOZE_ID:
                    Calendar in10Minutes = Calendar.getInstance();
                    in10Minutes.add(Calendar.MINUTE, 10);
                    AlarmUtils.createAlarm(this, key, title, content, in10Minutes);
                    break;
                case NotificationUtils.ARG_REMOVE_ID:
                    AlarmUtils.removeAlarm(this, key, title, content);
                    break;
            }
        }
    }
}
