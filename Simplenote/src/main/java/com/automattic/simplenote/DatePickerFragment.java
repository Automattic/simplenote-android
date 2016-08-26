package com.automattic.simplenote;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by jegumi on 24/08/16.
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private static final String ARG_DATE = "date";
    private Calendar calendar;

    public static DatePickerFragment newInstance(long timestamp) {
        DatePickerFragment fragment = new DatePickerFragment();
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

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Intent intent = new Intent();
        intent.putExtra(ReminderBottomSheetDialog.TIMESTAMP_BUNDLE_KEY, calendar.getTimeInMillis());
        intent.putExtra(ReminderBottomSheetDialog.REMINDER_ACTION_KEY, ReminderBottomSheetDialog.REMINDER_ACTION_DATE);
        getTargetFragment().onActivityResult(getTargetRequestCode(), ReminderBottomSheetDialog.UPDATE_REMINDER_REQUEST_CODE, intent);
    }
}