package com.automattic.simplenote.models;

import java.util.Calendar;

public class Reference {
    private Calendar mDate;
    private String mKey;
    private String mTitle;
    private int mCount;

    public Reference(String key, String title, Calendar date, int count) {
        mKey = key;
        mTitle = title;
        mDate = date;
        mCount = count;
    }

    public int getCount() {
        return mCount;
    }

    public Calendar getDate() {
        return mDate;
    }

    public String getKey() {
        return mKey;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setCount(int count) {
        mCount = count;
    }

    public void setDate(Calendar date) {
        mDate = date;
    }

    public void setKey(String key) {
        mKey = key;
    }

    public void setTitle(String title) {
        mTitle = title;
    }
}
