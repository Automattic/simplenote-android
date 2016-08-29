package com.automattic.simplenote.utils;

import android.content.Context;

import com.automattic.simplenote.models.Reminder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class ReminderDataSource
{
    private  ArrayList<Reminder> mList = null;
    private  long mNextId;

    private static final String DATA_FILE_NAME = "simplenote.txt";
    private static final long MAGIC_NUMBER = 0x54617200025641L;

    public ReminderDataSource getDataSource()
    {
        load();
        return this;
    }

    private  void load()
    {
        mList = new ArrayList<>();
        mNextId = 1;

        try
        {
            DataInputStream dis = new DataInputStream(ReminderListAdapter.mContext.openFileInput(DATA_FILE_NAME));
            long magic = dis.readLong();
            int size;

            if (MAGIC_NUMBER == magic)
            {
                mNextId = dis.readLong();
                size = dis.readInt();

                for (int i = 0; i < size; i++)
                {
                    Reminder reminder = new Reminder("");
                    reminder.deserialize(dis);
                    mList.add(reminder);
                }
            }

            dis.close();
        } catch (IOException e)
        {
            System.out.println(e.toString());
        }
    }

    public  void save()
    {

        try
        {
            DataOutputStream dos = new DataOutputStream(ReminderListAdapter.mContext.openFileOutput(DATA_FILE_NAME, Context.MODE_PRIVATE));
            dos.writeLong(MAGIC_NUMBER);
            dos.writeLong(mNextId);
            dos.writeInt(mList.size());

            for (int i = 0; i < mList.size(); i++)
                mList.get(i).serialize(dos);

            dos.close();
        } catch (IOException e)
        {
        }
    }

    public  int size()
    {
        return mList.size();
    }

    public  Reminder get(int position)
    {
        return mList.get(position);
    }

    public  void add(Reminder reminder)
    {
        reminder.setId(mNextId++);
        mList.add(reminder);
        Collections.sort(mList);
        save();
    }

    public  void remove(int index)
    {
        mList.remove(index);
        save();
    }

    public  void update(Reminder reminder)
    {
        reminder.update();
        Collections.sort(mList);
        save();
    }
}

