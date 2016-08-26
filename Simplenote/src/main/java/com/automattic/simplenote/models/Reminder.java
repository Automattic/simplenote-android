package com.automattic.simplenote.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by jegumi on 24/08/16.
 */
public class Reminder {

    private String date;
    private String time;

    public Reminder(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        if (timestamp != 0) {
            calendar.setTimeInMillis(timestamp);
        }

        date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(calendar.getTime());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        time = timeFormat.format(calendar.getTime());
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
