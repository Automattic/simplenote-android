package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by dan on 7/10/13.
 */
public class AnimatedLinearLayout extends LinearLayout
{
    private static final String TAG = AnimatedLinearLayout.class.getName();

    public AnimatedLinearLayout(Context context) {
        super(context);
    }

    public AnimatedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public float getXFraction()
    {
        int width =  ((Activity) getContext()).getWindowManager().getDefaultDisplay().getWidth();
        return (width == 0) ? 0 : getX() / (float) width;
    }

    public void setXFraction(float xFraction) {
        int width = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getWidth();
        setX((width > 0) ? (xFraction * width) : 0);
    }
}
