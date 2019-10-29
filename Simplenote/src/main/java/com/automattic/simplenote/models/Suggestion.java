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

    private String mName;
    private @Type int mType;

    public Suggestion(String name, @Type int type) {
        mName = name;
        mType = type;
    }

    public String getName() {
        return mName;
    }

    public @Type int getType() {
        return mType;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setType(@Type int type) {
        mType = type;
    }
}
