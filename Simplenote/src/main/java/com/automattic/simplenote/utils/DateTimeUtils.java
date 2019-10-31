package com.automattic.simplenote.utils;

import android.app.Activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    private static final ThreadLocal<DateFormat> ISO8601_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        }
    };

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

    /**
     * Given a {@link Date}, returns an ISO 8601-formatted {@link String}.
     */
    public static String getISO8601FromDate(Date date) {
        if (date == null) {
            return "";
        }

        DateFormat formatter = ISO8601_FORMAT.get();
        return formatter != null ? formatter.format(date) : "";
    }
}
