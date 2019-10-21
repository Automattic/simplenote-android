package com.automattic.simplenote.utils;

import android.app.Activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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

    public static String getDateTextShort(Calendar date) {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d", Locale.getDefault());
        return formatter.format(date.getTime());
    }
}
