package com.automattic.simplenote.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.ReminderFragment;
import com.automattic.simplenote.models.Reminder;
import com.automattic.simplenote.models.DateTime;

public class ReminderListAdapter extends BaseAdapter
{

    static Context mContext;
    private ReminderDataSource mReminderDataSource;
    private LayoutInflater mInflater;
    private DateTime mDateTime;
    private int mColorOutdated;
    private int mColorActive;
    private AlarmManager mAlarmManager;
    private String noteID;

    public ReminderListAdapter(Context context,String noteID)
    {
        mContext = context;
        this.noteID = noteID;

        mReminderDataSource = new ReminderDataSource();
        mReminderDataSource.getDataSource();


        mInflater = LayoutInflater.from(context);
        mDateTime = new DateTime(context);

        mColorOutdated = mContext.getResources().getColor(R.color.black);
        mColorActive = mContext.getResources().getColor(R.color.holo_blue_dark);

        mAlarmManager = (AlarmManager)context.getSystemService(mContext.ALARM_SERVICE);

        dataSetChanged();


    }

    public void save()
    {
        mReminderDataSource.save();
    }

    public void update(Reminder reminder)
    {
        mReminderDataSource.update(reminder);
        dataSetChanged();
    }

    public void updateAlarms()
    {
        for (int i = 0; i < mReminderDataSource.size(); i++)
            mReminderDataSource.update(mReminderDataSource.get(i));
        dataSetChanged();
    }

    public void add(Reminder reminder)
    {
        mReminderDataSource.add(reminder);
        dataSetChanged();
    }

    public void delete(int index)
    {
        cancelAlarm(mReminderDataSource.get(index));
        mReminderDataSource.remove(index);
        dataSetChanged();
    }

    public int getCount()
    {
        return mReminderDataSource.size();
    }

    public Reminder getItem(int position)
    {
        return mReminderDataSource.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
        Reminder reminder = mReminderDataSource.get(position);

        if (reminder.getNoteID().equals(noteID))
        {
            ReminderFragment.hideTV();

            convertView = mInflater.inflate(R.layout.reminders_list, null);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.item_title);
            holder.details = (TextView) convertView.findViewById(R.id.item_details);
            convertView.setTag(holder);

            holder.title.setText(reminder.getTitle());
            holder.details.setText(mDateTime.formatDetails(reminder) + (reminder.getEnabled() ? "" : " [disabled]"));

            if (reminder.getOutdated())
                holder.title.setTextColor(mColorOutdated);
            else
                holder.title.setTextColor(mColorActive);
        }
        else
            // return an empty view
            convertView = mInflater.inflate(R.layout.null_row_reminders_list, null);

        return convertView;
    }


    private void dataSetChanged()
    {
        for (int i = 0; i < mReminderDataSource.size(); i++)

        {
            setAlarm(mReminderDataSource.get(i));
        }
        notifyDataSetChanged();
    }

    private void setAlarm(Reminder reminder)
    {
        PendingIntent sender;
        Intent intent;

        if (reminder.getEnabled() && !reminder.getOutdated())
        {
            intent = new Intent(mContext, ReminderReceiver.class);
            reminder.toIntent(intent);
            sender = PendingIntent.getBroadcast(mContext, (int) reminder.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getDate(), sender);
        }
    }

    private void cancelAlarm(Reminder reminder)
    {
        PendingIntent sender;
        Intent intent;
        intent = new Intent(mContext, ReminderReceiver.class);
        sender = PendingIntent.getBroadcast(mContext, (int) reminder.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.cancel(sender);
    }

    static class ViewHolder
    {
        TextView title;
        TextView details;
    }
}

