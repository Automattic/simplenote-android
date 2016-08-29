package com.automattic.simplenote.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Reminder;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

public class ReminderReceiver extends BroadcastReceiver
{

    protected Bucket<Note> mNotesBucket;
    private String noteTitle;

    @Override
    public void onReceive(Context context, Intent intent) {
        Reminder reminder = new Reminder("");
        reminder.fromIntent(intent);

        Simplenote currentApp = (Simplenote) context.getApplicationContext();
        if (mNotesBucket == null) {
            mNotesBucket = currentApp.getNotesBucket();
        }

        try {
            noteTitle = mNotesBucket.get(reminder.getNoteID()).getTitle();
        } catch (BucketObjectMissingException e) {
            e.printStackTrace();
        }

        Intent editNoteIntent = new Intent(context, NotesActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(context, Simplenote.INTENT_EDIT_NOTE, editNoteIntent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentTitle("Simplenote")
                .setContentText(noteTitle)
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());

    }

}

