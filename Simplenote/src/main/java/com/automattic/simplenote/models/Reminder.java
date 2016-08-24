package com.automattic.simplenote.models;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by jegumi on 24/08/16.
 */
public class Reminder {

    private String date;
    private String time;

    public Reminder(long timestamp) {
        Date reminderDate = timestamp == 0 ? new Date() : new Date(timestamp);
        date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(reminderDate);
        time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(reminderDate);
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
