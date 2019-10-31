package com.automattic.simplenote.models;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@SuppressWarnings("unused")
public class Suggestion {
    @Retention(SOURCE)
    @IntDef({
        Type.HISTORY,
        Type.QUERY,
        Type.TAG
    })
    public @interface Type {
        int HISTORY = 0;
        int QUERY = 1;
        int TAG = 2;
    }

    private String mDate;
    private String mName;
    private @Type int mType;

    public Suggestion(String date, String name, @Type int type) {
        mDate = date;
        mName = name;
        mType = type;
    }

    public String getDate() {
        return mDate;
    }

    public String getName() {
        return mName;
    }

    public @Type int getType() {
        return mType;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setType(@Type int type) {
        mType = type;
    }
}
