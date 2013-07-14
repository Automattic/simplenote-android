package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * Created by dan on 7/10/13.
 */
public class AnimatedScrollView extends ScrollView
{
    private static final String TAG = AnimatedScrollView.class.getName();

    public AnimatedScrollView(Context context) {
        super(context);
    }

    public AnimatedScrollView(Context context, AttributeSet attrs) {
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
