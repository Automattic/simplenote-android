package com.automattic.simplenote.models;

import android.content.Intent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class Reminder implements Comparable<Reminder>
{
    public static final int ONCE = 0;
    public static final int WEEKLY = 1;
    public static final int NEVER = 0;
    public static final int EVERY_DAY = 0x7f;
    private long mId;
    private String noteID;
    private long mDate;
    private int mOccurence;
    private int mDays;
    private long mNextOccurence;



    public Reminder(String noteID)
    {
        mId = 0;
        this.noteID=noteID;
        mDate = System.currentTimeMillis();
        mOccurence = ONCE;
        mDays = EVERY_DAY;
        update();
    }

    public long getId()
    {
        return mId;
    }

    public void setId(long id)
    {
        mId = id;
    }


    public int getOccurence()
    {
        return mOccurence;
    }

    public void setOccurence(int occurence)
    {
        mOccurence = occurence;
        update();
    }

    public long getDate()
    {
        return mDate;
    }

    public void setDate(long date)
    {
        mDate = date;
        update();
    }

    public int getDays()
    {
        return mDays;
    }

    public void setDays(int days)
    {
        mDays = days;
        update();
    }
    public String getNoteID() {
        return noteID;
    }

    public void setNoteID(String noteID) {
        this.noteID = noteID;
    }

    public long getNextOccurence()
    {
        return mNextOccurence;
    }

    public boolean getOutdated()
    {
        return mNextOccurence < System.currentTimeMillis();
    }

    public int compareTo(Reminder aThat)
    {
        final long thisNext = getNextOccurence();
        final long thatNext = aThat.getNextOccurence();
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == aThat)
            return EQUAL;

        if (thisNext > thatNext)
            return AFTER;
        else if (thisNext < thatNext)
            return BEFORE;
        else
            return EQUAL;
    }

    public void update()
    {
        Calendar now = Calendar.getInstance();

        if (mOccurence == WEEKLY)
        {
            Calendar alarm = Calendar.getInstance();

            alarm.setTimeInMillis(mDate);
            alarm.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

            if (mDays != NEVER)
            {
                while (true)
                {
                    int day = (alarm.get(Calendar.DAY_OF_WEEK) + 5) % 7;

                    if (alarm.getTimeInMillis() > now.getTimeInMillis() && (mDays & (1 << day)) > 0)
                        break;

                    alarm.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
            else
            {
                alarm.add(Calendar.YEAR, 10);
            }

            mNextOccurence = alarm.getTimeInMillis();
        }
        else
        {
            mNextOccurence = mDate;
        }

        mDate = mNextOccurence;
    }

    public void toIntent(Intent intent)
    {
        intent.putExtra("id", mId);
        intent.putExtra("noteid",noteID);
        intent.putExtra("date", mDate);
        intent.putExtra("occurence", mOccurence);
        intent.putExtra("days", mDays);
    }

    public void fromIntent(Intent intent)
    {
        mId = intent.getLongExtra("id", 0);
        noteID=intent.getStringExtra("noteid");
        mDate = intent.getLongExtra("date", 0);
        mOccurence = intent.getIntExtra("occurence", 0);
        mDays = intent.getIntExtra("days", 0);
        update();
    }

    public void serialize(DataOutputStream dos) throws IOException
    {
        dos.writeLong(mId);
        dos.writeUTF(noteID);
        dos.writeLong(mDate);
        dos.writeInt(mOccurence);
        dos.writeInt(mDays);
    }

    public void deserialize(DataInputStream dis) throws IOException
    {
        mId = dis.readLong();
        noteID = dis.readUTF();
        mDate = dis.readLong();
        mOccurence = dis.readInt();
        mDays = dis.readInt();
        update();
    }
}

