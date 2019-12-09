package com.automattic.simplenote.utils;

import android.app.Activity;
import android.text.format.DateFormat;

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

    public static String getDateTextNumeric(Calendar date) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MM/dd/YYYY");
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date.getTime());
    }
}
