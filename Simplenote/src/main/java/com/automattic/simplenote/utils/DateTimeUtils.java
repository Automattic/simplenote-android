package com.automattic.simplenote.utils;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeUtils {
    public static String getDateText(Context context, Calendar noteDate) {
        if (noteDate == null) {
            return "";
        }

        long now = Calendar.getInstance().getTimeInMillis();
        CharSequence dateText = DateUtils.getRelativeDateTimeString(
                context,
                noteDate.getTimeInMillis(),
                now,
                0L,
                DateUtils.FORMAT_ABBREV_ALL
        );

        return dateText.toString();
    }

    public static String getDateTextNumeric(Calendar date) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MM/dd/YYYY");
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date.getTime());
    }
}
