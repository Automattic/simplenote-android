package com.automattic.simplenote.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsEnabled) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return this.mIsEnabled && super.performClick();
    }

    public void setPagingEnabled(boolean enabled) {
        this.mIsEnabled = enabled;
    }
}
