package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.automattic.simplenote.utils.ReminderListAdapter;

/**
 * Created by Marwen Doukh on 23/08/2016.
 */


public class ReminderFragment extends Fragment {

    static TextView noReminderFound;
    private final int NEW_ALARM_ACTIVITY = 0;
    private final int EDIT_ALARM_ACTIVITY = 1;
    private ListView mAlarmList;
    private ReminderListAdapter mReminderListAdapter;
    private com.automattic.simplenote.models.Reminder mCurrentReminder;
    private FloatingActionButton fab;
    private String noteID ;
    private AdapterView.OnItemClickListener mListOnItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Intent intent = new Intent(getActivity().getBaseContext(), EditReminderActivity.class);
            mCurrentReminder = mReminderListAdapter.getItem(position);
            mCurrentReminder.toIntent(intent);
            ReminderFragment.this.startActivityForResult(intent, EDIT_ALARM_ACTIVITY);
        }
    };

    public ReminderFragment(){}

    public static void hideTV() {
        noReminderFound.setVisibility(View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reminder, container, false);

        mAlarmList = (ListView) view.findViewById(R.id.alarm_list);
        noteID = getActivity().getIntent().getStringExtra("noteid");

        mReminderListAdapter = new ReminderListAdapter(getActivity().getApplicationContext(),noteID);
        mAlarmList.setAdapter(mReminderListAdapter);
        mAlarmList.setOnItemClickListener(mListOnItemClickListener);

        registerForContextMenu(mAlarmList);
        mCurrentReminder = null;

        fab = (FloatingActionButton) view.findViewById(R.id.add_reminder_fab);
        fab.setBackgroundTintList(getResources().getColorStateList(R.color.simplenote_blue));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity().getApplicationContext(), EditReminderActivity.class);
                mCurrentReminder = new com.automattic.simplenote.models.Reminder(noteID);
                mCurrentReminder.toIntent(intent);
                startActivityForResult(intent, NEW_ALARM_ACTIVITY);

            }
        });

        noReminderFound = (TextView) view.findViewById(R.id.no_reminder_found);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mReminderListAdapter.updateAlarms();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == NEW_ALARM_ACTIVITY) {
            if (resultCode == getActivity().RESULT_OK) {
                mCurrentReminder.fromIntent(data);
                mReminderListAdapter.add(mCurrentReminder);
            }
            mCurrentReminder = null;
        } else if (requestCode == EDIT_ALARM_ACTIVITY) {
            if (resultCode == getActivity().RESULT_OK) {
                mCurrentReminder.fromIntent(data);
                mReminderListAdapter.update(mCurrentReminder);
            }
            mCurrentReminder = null;
        }
    }

}

