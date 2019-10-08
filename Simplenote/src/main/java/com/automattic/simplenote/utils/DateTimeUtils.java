package com.automattic.simplenote.utils;

import android.app.Activity;

import java.util.Calendar;

public class DateTimeUtils {
    public static String getDateText(Activity activity, Calendar noteDate) {
        if (noteDate == null) {
            return "";
        }

        long now = Calendar.getInstance().getTimeInMillis();
        CharSequence dateText = android.text.format.DateUtils.getRelativeDateTimeString(
                activity,
                noteDate.getTimeInMillis(),
                now,
                0L,
                android.text.format.DateUtils.FORMAT_ABBREV_ALL
        );

        return dateText.toString();
    }
}
