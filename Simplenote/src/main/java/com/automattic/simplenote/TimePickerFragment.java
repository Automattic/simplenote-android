package com.automattic.simplenote;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by jegumi on 24/08/16.
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private static final String ARG_DATE = "date";
    private Calendar calendar;

    public static TimePickerFragment newInstance(long timestamp) {
        TimePickerFragment fragment = new TimePickerFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DATE, timestamp);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        long timestamp = getArguments().getLong(ARG_DATE);

        calendar = new GregorianCalendar();
        calendar.setTime(new Date(timestamp));

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), this, hour, minute, true);
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent();
        intent.putExtra(ReminderBottomSheetDialog.TIMESTAMP_BUNDLE_KEY, calendar.getTimeInMillis());
        intent.putExtra(ReminderBottomSheetDialog.REMINDER_ACTION_KEY, ReminderBottomSheetDialog.REMINDER_ACTION_TIME);
        getTargetFragment().onActivityResult(getTargetRequestCode(), ReminderBottomSheetDialog.UPDATE_REMINDER_REQUEST_CODE, intent);
    }
}