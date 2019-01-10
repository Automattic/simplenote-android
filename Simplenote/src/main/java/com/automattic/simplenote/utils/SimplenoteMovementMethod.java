package com.automattic.simplenote.utils;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

import com.automattic.simplenote.widgets.CheckableSpan;

// Only allows onClick events for CheckableSpans
public class SimplenoteMovementMethod extends LinkMovementMethod {

    private static SimplenoteMovementMethod mInstance;

    public static SimplenoteMovementMethod getInstance() {
        if (mInstance == null)
            mInstance = new SimplenoteMovementMethod();
        return mInstance;
    }
    
    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            CheckableSpan[] checkableSpans = buffer.getSpans(off, off + 1, CheckableSpan.class);

            if (checkableSpans.length != 0) {
                checkableSpans[0].onClick(widget);

                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }

        return false;
    }
}
