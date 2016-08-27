
package com.automattic.simplenote;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.automattic.simplenote.models.Reminder;
import com.automattic.simplenote.models.DateTime;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Marwen Doukh on 23/08/2016.
 */

public class EditReminderFragment extends Fragment implements com.wdullaer.materialdatetimepicker.time.TimePickerDialog.OnTimeSetListener, com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener
{
    private EditText mTitle;
    private CheckBox mAlarmEnabled;
    private Spinner mOccurence;
    private Button mDateButton;
    private Button mTimeButton;
    private Reminder mReminder;
    private DateTime mDateTime;
    private Calendar mCalendar;
    private int mYear;
    private int mMonth;
    private int mDay;
    private int mHour;
    private int mMinute;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_edit_reminder, container, false);
        mTitle = (EditText) view.findViewById(R.id.title);
        mAlarmEnabled = (CheckBox) view.findViewById(R.id.alarm_checkbox);
        mOccurence = (Spinner) view.findViewById(R.id.occurence_spinner);
        mDateButton = (Button) view.findViewById(R.id.date_button);
        mTimeButton = (Button) view.findViewById(R.id.time_button);

        // Buttons Listeners
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Reminder.ONCE == mReminder.getOccurence()) {
                    com.wdullaer.materialdatetimepicker.date.DatePickerDialog dpd = com.wdullaer.materialdatetimepicker.date.DatePickerDialog.newInstance(
                            EditReminderFragment.this,
                            mCalendar.get(Calendar.YEAR),
                            mCalendar.get(Calendar.MONTH),
                            mCalendar.get(Calendar.DAY_OF_MONTH)
                    );

                    dpd.show(getFragmentManager(), "Datepickerdialog");
                }
                else if (Reminder.WEEKLY == mReminder.getOccurence())
                {
                    AlertDialog.Builder builder;
                    final boolean[] days = mDateTime.getDays(mReminder);
                    final String[] names = mDateTime.getFullDayNames();

                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Week days");
                    builder.setMultiChoiceItems(names, days, new DialogInterface.OnMultiChoiceClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton, boolean isChecked)
                        {
                        }
                    });

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            mDateTime.setDays(mReminder, days);
                            updateButtons();
                        }
                    });

                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                }

            }
        });

        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                com.wdullaer.materialdatetimepicker.time.TimePickerDialog  tpd = com.wdullaer.materialdatetimepicker.time.TimePickerDialog.newInstance(
                        EditReminderFragment.this,
                        mCalendar.get(Calendar.HOUR_OF_DAY),
                        mCalendar.get(Calendar.MINUTE),
                        mDateTime.is24hClock()
                );
                tpd.show(getFragmentManager(), "Timepickerdialog");
            }
        });


        getActivity().invalidateOptionsMenu();
        setHasOptionsMenu(true);

        mReminder = new Reminder("");
        mReminder.fromIntent(getActivity().getIntent());

        mDateTime = new DateTime(getActivity());

        mTitle.setText(mReminder.getTitle());
        mTitle.addTextChangedListener(mTitleChangedListener);

        mOccurence.setSelection(mReminder.getOccurence());
        mOccurence.setOnItemSelectedListener(mOccurenceSelectedListener);

        mAlarmEnabled.setChecked(mReminder.getEnabled());
        mAlarmEnabled.setOnCheckedChangeListener(mAlarmEnabledChangeListener);

        mCalendar =  Calendar.getInstance();

        mCalendar.setTimeInMillis(mReminder.getDate());
        mYear = mCalendar.get(Calendar.YEAR);
        mMonth = mCalendar.get(Calendar.MONTH);
        mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
        mHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        mMinute = mCalendar.get(Calendar.MINUTE);

        updateButtons();

        return view;
    }



    private TextWatcher mTitleChangedListener = new TextWatcher()
    {
        public void afterTextChanged(Editable s)
        {
            mReminder.setTitle(mTitle.getText().toString());
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
        }
    };

    private AdapterView.OnItemSelectedListener mOccurenceSelectedListener = new AdapterView.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
        {
            mReminder.setOccurence(position);
            updateButtons();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    };

    private CompoundButton.OnCheckedChangeListener mAlarmEnabledChangeListener = new CompoundButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            mReminder.setEnabled(isChecked);
        }
    };

    private void updateButtons()
    {
        if (Reminder.ONCE == mReminder.getOccurence())
            mDateButton.setText(mDateTime.formatDate(mReminder));
        else if (Reminder.WEEKLY == mReminder.getOccurence())
            mDateButton.setText(mDateTime.formatDays(mReminder));
        mTimeButton.setText(mDateTime.formatTime(mReminder));
    }


    @Override
    public void onDateSet(com.wdullaer.materialdatetimepicker.date.DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        System.out.println("You picked the following date: "+dayOfMonth+"/"+(monthOfYear+1)+"/"+year);
        mYear = year;
        mMonth = monthOfYear;
        mDay = dayOfMonth;

        mCalendar = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute);
        mReminder.setDate(mCalendar.getTimeInMillis());

        updateButtons();
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        System.out.println("You picked the following time: "+hourOfDay+"h"+minute);
        mHour = hourOfDay;
        mMinute = minute;

        mCalendar = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute);
        mReminder.setDate(mCalendar.getTimeInMillis());
        updateButtons();

    }
    ///// options menu
    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.edit_reminder, menu);
        DrawableUtils.tintMenuWithAttribute(getActivity(), menu, R.attr.actionBarTextColor);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.done:
                Intent intent = new Intent();
                mReminder.toIntent(intent);
                getActivity().setResult(getActivity().RESULT_OK, intent);
                getActivity().finish();
                return true;

            case android.R.id.home:
                getActivity().finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

