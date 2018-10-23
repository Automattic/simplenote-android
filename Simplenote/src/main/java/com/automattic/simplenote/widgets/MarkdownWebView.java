package com.automattic.simplenote.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class MarkdownWebView extends WebView {

    public MarkdownWebView(Context context) {
        super(context);
    }

    public MarkdownWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        requestDisallowInterceptTouchEvent(false);
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            requestDisallowInterceptTouchEvent(true);
            return true;
        }

        return true;
    }
}
