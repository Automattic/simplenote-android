package com.automattic.simplenote;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Reminder;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Jesus Gumiel on 24/08/2016.
 */
public class ReminderBottomSheetDialog extends BottomSheetDialogBase implements View.OnClickListener {

    public static final int UPDATE_REMINDER_REQUEST_CODE = 101;
    public static final String TIMESTAMP_BUNDLE_KEY = "reminder";
    public static final String REMINDER_ACTION_KEY = "reminderAction";
    public static final int REMINDER_ACTION_DATE = 0;
    public static final int REMINDER_ACTION_TIME = 1;

    private static final long REMINDER_DELAY = 10 * DateUtils.MINUTE_IN_MILLIS;

    private ReminderSheetListener mReminderSheetListener;
    private Switch mReminderSwitch;
    private TextView mDateTextView;
    private TextView mTimeTextView;
    private TextView mSnoozeTextView;

    private Fragment mFragment;
    private Note mNote;
    private long timestamp;

    public ReminderBottomSheetDialog(@NonNull final Fragment fragment, @NonNull ReminderSheetListener reminderSheetListener) {
        super(fragment.getActivity());

        mFragment = fragment;
        mReminderSheetListener = reminderSheetListener;

        View reminderView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.bottom_sheet_reminder, null, false);
        mReminderSwitch = (Switch) reminderView.findViewById(R.id.reminder_switch);
        mDateTextView = (TextView) reminderView.findViewById(R.id.date_reminder);
        mTimeTextView = (TextView) reminderView.findViewById(R.id.time_reminder);
        mSnoozeTextView = (TextView) reminderView.findViewById(R.id.snooze_reminder);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mReminderSheetListener.onReminderDismissed();
            }
        });

        setContentView(reminderView);
    }

    public void show(Note note) {
        mNote = note;
        if (mFragment.isAdded()) {
            mDateTextView.setOnClickListener(this);
            mTimeTextView.setOnClickListener(this);
            initReminder(mNote.getReminderDate().getTimeInMillis());
            show();
        }
    }

    private void initReminder(long timestamp) {
        if (mNote.hasReminder()) {
            refreshReminder(timestamp);
        } else {
            refreshReminder(Calendar.getInstance().getTimeInMillis() + REMINDER_DELAY);
        }
        mReminderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mReminderSheetListener.onReminderOn();
                } else {
                    mReminderSheetListener.onReminderOff();
                    mSnoozeTextView.setVisibility(View.GONE);
                }
            }
        });
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.timestamp);
        mReminderSheetListener.onReminderUpdated(calendar, false);
    }

    private void refreshReminder(long timestamp) {
        this.timestamp = timestamp;

        Reminder reminder = new Reminder(timestamp);
        mDateTextView.setText(reminder.getDate());
        mTimeTextView.setText(reminder.getTime());
        mReminderSwitch.setChecked(mNote.hasReminder());

        if (mNote.hasReminder()) {
            Calendar snoozeDate = mNote.getSnoozeDate();
            if (snoozeDate != null) {
                mSnoozeTextView.setVisibility(View.VISIBLE);
                mSnoozeTextView.setText(getContext().getString(R.string.reminder_snooze_text,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                                .format(snoozeDate.getTimeInMillis())));
            } else {
                mSnoozeTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(View v) {
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

        }
    }

    public void updateReminder(Calendar calendar) {
        mNote.setReminderDate(calendar);
        refreshReminder(mNote.getReminderDate().getTimeInMillis());
    }

    public interface ReminderSheetListener {
        void onReminderOn();

        void onReminderOff();

        void onReminderUpdated(Calendar calendar, boolean updateAlarm);

        void onReminderDismissed();
    }
}
