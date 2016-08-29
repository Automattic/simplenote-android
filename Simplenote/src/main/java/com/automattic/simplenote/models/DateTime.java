package com.automattic.simplenote.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateTime
{
  private Context mContext;
  private String[] mFullDayNames;
  private String[] mShortDayNames;
  private boolean mWeekStartsOnMonday;
  private boolean m24hClock;
  private SimpleDateFormat mTimeFormat;
  private SimpleDateFormat mDateFormat;

  public DateTime(Context context)
  {
    mContext = context;
    update();
  }

  public void update()
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    mWeekStartsOnMonday = prefs.getBoolean("week_starts_pref", false);
    m24hClock = prefs.getBoolean("use_24h_pref", false);

    mDateFormat = new SimpleDateFormat("E MMM d, yyyy");

    if (m24hClock)
      mTimeFormat = new SimpleDateFormat("H:mm");
    else
      mTimeFormat = new SimpleDateFormat("h:mm a");

    mFullDayNames = new String[7];
    mShortDayNames = new String[7];

    SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE");
    SimpleDateFormat shortFormat = new SimpleDateFormat("E");
    Calendar calendar;

    if (mWeekStartsOnMonday)
      calendar = new GregorianCalendar(2012, Calendar.AUGUST, 6);
    else
      calendar = new GregorianCalendar(2012, Calendar.AUGUST, 5);

    for (int i = 0; i < 7; i++)
    {
      mFullDayNames[i] = fullFormat.format(calendar.getTime());
      mShortDayNames[i] = shortFormat.format(calendar.getTime());
      calendar.add(Calendar.DAY_OF_WEEK, 1);
    }
  }

  public boolean is24hClock()
  {
    return m24hClock;
  }

  public String formatTime(Reminder reminder)
  {
    return mTimeFormat.format(new Date(reminder.getDate()));
  }

  public String formatDate(Reminder reminder)
  {
    return mDateFormat.format(new Date(reminder.getDate()));
  }

  public String formatDays(Reminder reminder)
  {
    boolean[] days = getDays(reminder);
    String res = "";

    if (reminder.getDays() == reminder.NEVER)
      res = "Never";
    else if (reminder.getDays() == reminder.EVERY_DAY)
      res = "Every day";
    else
    {
      for (int i = 0; i < 7; i++)
        if (days[i])
          res += ("" == res) ? mShortDayNames[i] : ", " + mShortDayNames[i];
    }

    return res;
  }

  public String formatDetails(Reminder reminder)
  {
    String res = "???";

    if (reminder.getOccurence() == Reminder.ONCE)
      res = formatDate(reminder);
    else if (reminder.getOccurence() == Reminder.WEEKLY)
      res = formatDays(reminder);

    res += ", " + formatTime(reminder);

    return res;
  }

  public boolean[] getDays(Reminder reminder)
  {
    int offs = mWeekStartsOnMonday ? 0 : 1;
    boolean[] rDays = new boolean[7];
    int aDays = reminder.getDays();

    for (int i = 0; i < 7; i++)
      rDays[(i+offs) % 7] = (aDays & (1 << i)) > 0;

    return rDays;
  }

  public void setDays(Reminder reminder, boolean[] days)
  {
    int offs = mWeekStartsOnMonday ? 0 : 1;
    int sDays = 0;

    for (int i = 0; i < 7; i++)
      sDays |= days[(i+offs) % 7] ? (1 << i) : (0 << i);

    reminder.setDays(sDays);
  }

  public String[] getFullDayNames()
  {
    return mFullDayNames;
  }

}

