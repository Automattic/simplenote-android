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
    public boolean onTouchEvent(TextView textView, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= textView.getTotalPaddingLeft();
            y -= textView.getTotalPaddingTop();

            x += textView.getScrollX();
            y += textView.getScrollY();

            Layout layout = textView.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            int lineStart = layout.getLineStart(line);

            // Also toggle the checkbox if the user tapped the space next to the checkbox
            if (off == lineStart + 1) {
                off = lineStart;
            }

            CheckableSpan[] checkableSpans = buffer.getSpans(off, off + 1, CheckableSpan.class);

            if (checkableSpans.length != 0) {
                checkableSpans[0].onClick(textView);

                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }

        return false;
    }
}
