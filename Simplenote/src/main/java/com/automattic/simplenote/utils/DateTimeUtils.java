package com.automattic.simplenote.utils;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.text.ParseException;
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

    public static String getDateTextString(Context context, Calendar calendar) {
        String pattern = DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                DateFormat.is24HourFormat(context) ? "MMM dd, yyyy, H:mm" : "MMM dd, yyyy, h:mm"
        );
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.getTime());
    }

    public static Calendar getDateCalendar(String json) throws ParseException {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        Calendar date = Calendar.getInstance();
        date.setTime(dateFormat.parse(json));
        return date;
    }
}
