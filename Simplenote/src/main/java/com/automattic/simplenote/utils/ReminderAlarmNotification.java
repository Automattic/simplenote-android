package com.automattic.simplenote.utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.ReminderFragment;
import com.automattic.simplenote.models.DateTime;
import com.daimajia.swipe.SwipeLayout;

import java.util.Timer;
import java.util.TimerTask;

public class ReminderAlarmNotification extends Activity
{

    private Ringtone mRingtone;
    private Vibrator mVibrator;
    private final long[] mVibratePattern = { 0, 500, 500 };
    private boolean mVibrate;
    private Uri mAlarmSound;
    private long mPlayTime;
    private Timer mTimer = null;
    private com.automattic.simplenote.models.Reminder mReminder;
    private DateTime mDateTime;
    private TextView mTextView;
    private PlayTimerTask mTimerTask;


    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.reminder_notification);

        mDateTime = new DateTime(this);
        mTextView = (TextView)findViewById(R.id.alarm_title_text);

        readPreferences();

        mRingtone = RingtoneManager.getRingtone(getApplicationContext(), mAlarmSound);
        if (mVibrate)
            mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        start(getIntent());


        SwipeLayout swipeLayout =  (SwipeLayout)findViewById(R.id.dismiss);

//set show mode.
        swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);

//add drag edge.(If the BottomView has 'layout_gravity' attribute, this line is unnecessary)
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, findViewById(R.id.bottom_wrapper));

        swipeLayout.addSwipeListener(new SwipeLayout.SwipeListener() {
            @Override
            public void onClose(SwipeLayout layout) {
                //when the SurfaceView totally cover the BottomView.
            }

            @Override
            public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {
                //you are swiping.
            }

            @Override
            public void onStartOpen(SwipeLayout layout) {

            }

            @Override
            public void onOpen(SwipeLayout layout) {
                //when the BottomView totally show.
                System.out.println("dismiss alarm");
                LinearLayout background = (LinearLayout) findViewById(R.id.background);
                background.setBackgroundColor(getResources().getColor(R.color.dismiss_reminder_color));
                finish();
            }

            @Override
            public void onStartClose(SwipeLayout layout) {

            }

            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
                //when user's hand released.
            }
        });

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stop();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        addNotification(mReminder);
        stop();
        start(intent);
    }

    private void start(Intent intent)
    {
        mReminder = new com.automattic.simplenote.models.Reminder("");
        mReminder.fromIntent(intent);

        mTextView.setText(mReminder.getTitle());

        mTimerTask = new PlayTimerTask();
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, mPlayTime);
        mRingtone.play();
        if (mVibrate)
            mVibrator.vibrate(mVibratePattern, 0);
    }

    private void stop()
    {
        mTimer.cancel();
        mRingtone.stop();
        if (mVibrate)
            mVibrator.cancel();
    }



    private void readPreferences()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mAlarmSound = Uri.parse(prefs.getString("alarm_sound_pref", "DEFAULT_RINGTONE_URI"));
        mVibrate = prefs.getBoolean("vibrate_pref", true);
        mPlayTime = (long)Integer.parseInt(prefs.getString("alarm_play_time_pref", "30")) * 1000;
    }

    private void addNotification(com.automattic.simplenote.models.Reminder reminder)
    {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        PendingIntent activity;
        Intent intent;

        intent = new Intent(this.getApplicationContext(), ReminderFragment.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        activity = PendingIntent.getActivity(this, (int) reminder.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        notification = builder
                .setContentIntent(activity)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentTitle(reminder.getTitle())
                .setContentText(mDateTime.formatDetails(reminder))
                .build();

        notificationManager.notify((int) reminder.getId(), notification);
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }

    private class PlayTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            addNotification(mReminder);
            finish();
        }
    }
}

