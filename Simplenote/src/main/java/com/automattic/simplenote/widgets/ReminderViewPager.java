package com.automattic.simplenote.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ReminderViewPager extends ViewPager {
    private boolean mIsEnabled;

    public ReminderViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsEnabled = true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mIsEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.mIsEnabled && super.onTouchEvent(event);
    }

}
