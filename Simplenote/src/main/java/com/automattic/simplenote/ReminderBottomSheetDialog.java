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
    private TextView m15MinDateTextView;
    private TextView m15MinTimeTextView;
    private TextView mHourDateTextView;
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
                break;
            case R.id.after_hour:
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(Calendar.getInstance().getTime());
                calendar.add(Calendar.HOUR, 1);
                reminderSheetListener.onReminderUpdated(calendar);
                break;
        }
        mReminderSwitch.setChecked(true);
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