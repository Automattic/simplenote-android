package com.automattic.simplenote;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Reminder;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by asmadek on 28/09/2016.
 */
public class ReminderBottomSheetDialog extends BottomSheetDialogBase implements View.OnClickListener {

    public static final int UPDATE_REMINDER_REQUEST_CODE = 101;
    public static final String TIMESTAMP_BUNDLE_KEY = "reminder";

    private Switch mReminderSwitch;
    private TextView mDateTextView;
    private TextView mTimeTextView;
    private TextView m15MinTimeTextView;
    private TextView mHourTimeTextView;


    private Fragment mFragment;
    private Note mNote;

    private ReminderSheetListener reminderSheetListener;

    public ReminderBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final ReminderSheetListener _reminderSheetListener) {
        super(fragment.getActivity());

        reminderSheetListener = _reminderSheetListener;

        mFragment = fragment;

        View reminderView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.bottom_sheet_reminder, null, false);
        mReminderSwitch = (Switch) reminderView.findViewById(R.id.reminder_switch);
        mDateTextView = (TextView) reminderView.findViewById(R.id.date_reminder);
        mTimeTextView = (TextView) reminderView.findViewById(R.id.time_reminder);
        m15MinTimeTextView = (TextView) reminderView.findViewById(R.id.after_15_minutes);
        mHourTimeTextView = (TextView) reminderView.findViewById(R.id.after_hour);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                reminderSheetListener.onReminderDismissed();
            }
        });

        mReminderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    reminderSheetListener.onReminderOn();
                } else {
                    reminderSheetListener.onReminderOff();
                }
            }
        });

        setContentView(reminderView);
    }

    public void show(Note note) {
        mNote = note;
        if (mFragment.isAdded()) {
            mDateTextView.setOnClickListener(this);
            mTimeTextView.setOnClickListener(this);
            m15MinTimeTextView.setOnClickListener(this);
            mHourTimeTextView.setOnClickListener(this);
            refreshReminder();
            show();
        }
    }

    private void refreshReminder() {
        Reminder reminder = new Reminder(mNote.getReminderDate().getTimeInMillis());
        mDateTextView.setText(reminder.getDate());
        mTimeTextView.setText(reminder.getTime());
        mReminderSwitch.setChecked(mNote.hasReminder());
    }


    @Override
    public void onClick(View v) {
        long timestamp = mNote.getReminderDate().getTimeInMillis();
        switch (v.getId()) {
            case R.id.date_reminder:
                DialogFragment dateFragment = DatePickerFragment.newInstance(timestamp);
                dateFragment.setTargetFragment(mFragment, UPDATE_REMINDER_REQUEST_CODE);
                dateFragment.show(mFragment.getFragmentManager(), "datePicker");
                break;
            case R.id.time_reminder:
                DialogFragment timeFragment = TimePickerFragment.newInstance(timestamp);
                timeFragment.setTargetFragment(mFragment, UPDATE_REMINDER_REQUEST_CODE);
                timeFragment.show(mFragment.getFragmentManager(), "timePicker");
                break;
            case R.id.after_15_minutes:
                Calendar after15min = new GregorianCalendar();
                after15min.setTime(Calendar.getInstance().getTime());
                after15min.add(Calendar.MINUTE, 15);
                reminderSheetListener.onReminderUpdated(after15min);
                enableReminder();
                break;
            case R.id.after_hour:
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(Calendar.getInstance().getTime());
                calendar.add(Calendar.HOUR, 1);
                reminderSheetListener.onReminderUpdated(calendar);
                enableReminder();
                break;
        }
    }

    public void enableReminder() {
        if (mReminderSwitch.isChecked())
            showPopup();
        else
            mReminderSwitch.setChecked(true);
        //showPopup(getContext().getString(R.string.reminder_is_set));
    }

    public void disableReminder() {
        mReminderSwitch.setChecked(false);
    }


    public void showPopup() {
        Date now = Calendar.getInstance().getTime();
        String delay = getTimeDifference(now, mNote.getReminderDate().getTime());

        if (!delay.equals("")) {
            String message = getContext().getString(R.string.reminder_will_raise_in) + " " + delay;
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        } else
            mReminderSwitch.setChecked(false);
    }

    public String getTimeDifference(Date startDate, Date endDate) {

        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        if (different < 0)
            return "";

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli + 1;


        long elapsedMinutes = different / minutesInMilli + 1;

        if (elapsedMinutes == 60) {
            elapsedMinutes = 0;
            elapsedHours++;
        }

        return (elapsedDays == 0 ? "" : Long.toString(elapsedDays) + " " + getContext().getString(R.string.of_days) + " ")
                + (elapsedHours == 0 ? "" : Long.toString(elapsedHours) + " " + getContext().getString(R.string.of_hours) + " ")
                + (elapsedMinutes == 0 ? "" : Long.toString(elapsedMinutes) + " " + getContext().getString(R.string.of_minutes));

    }

    public void updateReminder(Calendar calendar) {
        mNote.setReminderDate(calendar);
        refreshReminder();
    }

    public interface ReminderSheetListener {
        void onReminderOn();

        void onReminderOff();

        void onReminderUpdated(Calendar calendar);

        void onReminderDismissed();
    }
}