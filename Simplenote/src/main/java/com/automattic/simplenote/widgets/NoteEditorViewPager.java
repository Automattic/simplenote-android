package com.automattic.simplenote.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NoteEditorViewPager extends ViewPager {
    private boolean mIsEnabled;

    public NoteEditorViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsEnabled = true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mIsEnabled && super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.mIsEnabled && super.onTouchEvent(event);
    }

    public void setPagingEnabled(boolean enabled) {
        this.mIsEnabled = enabled;
    }
}
