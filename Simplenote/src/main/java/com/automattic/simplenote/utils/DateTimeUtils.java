package com.automattic.simplenote.utils;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeUtils {
    public static String getDateText(Context context, Calendar calendar) {
        if (calendar == null) {
            return "";
        }

        CharSequence dateText = DateUtils.getRelativeDateTimeString(
                context,
                calendar.getTimeInMillis(),
                Calendar.getInstance().getTimeInMillis(),
                0L,
                DateUtils.FORMAT_ABBREV_ALL
        );

        return dateText.toString();
    }

    public static String getDateTextNumeric(Calendar date) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MM/dd/yyyy");
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date.getTime());
    }
}
