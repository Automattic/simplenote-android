package com.automattic.simplenote.utils;

import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.Touch;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.automattic.simplenote.widgets.CheckableSpan;

// Only allows onClick events for CheckableSpans
public class SimplenoteMovementMethod extends ArrowKeyMovementMethod {
    private static SimplenoteMovementMethod mInstance;

    /* package */ static class CheckboxTouchState implements NoCopySpan {
        final float x;
        final float y;
        final CheckableSpan targetSpan;

        CheckboxTouchState(float x, float y, CheckableSpan targetSpan) {
            this.x = x;
            this.y = y;
            this.targetSpan = targetSpan;
        }
    }

    public static SimplenoteMovementMethod getInstance() {
        if (mInstance == null) {
            mInstance = new SimplenoteMovementMethod();
        }

        return mInstance;
    }

    @Override
    public boolean onTouchEvent(TextView textView, Spannable buffer, MotionEvent event) {
        if (textView.getLayout() == null) {
            return super.onTouchEvent(textView, buffer, event);
        }

        int touchSlop = ViewConfiguration.get(textView.getContext()).getScaledTouchSlop();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                CheckboxTouchState[] oldStates = buffer.getSpans(0, buffer.length(), CheckboxTouchState.class);
                for (CheckboxTouchState state : oldStates) {
                    buffer.removeSpan(state);
                }
                textView.setFocusableInTouchMode(true);

                int x = (int) event.getX() - textView.getTotalPaddingLeft() + textView.getScrollX();
                int y = (int) event.getY() - textView.getTotalPaddingTop() + textView.getScrollY();

                Layout layout = textView.getLayout();
                if (y < 0 || y >= layout.getHeight()) {
                    return super.onTouchEvent(textView, buffer, event);
                }

                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                int lineStart = layout.getLineStart(line);

                // Also toggle the checkbox if the user tapped the space next to the checkbox
                if (off == lineStart + 1) {
                    off = lineStart;
                }

                CheckableSpan[] checkableSpans = buffer.getSpans(off, off, CheckableSpan.class);
                if (checkableSpans.length != 0) {
                    if (!textView.hasFocus()) {
                        textView.setFocusableInTouchMode(false);
                    }
                    buffer.setSpan(new CheckboxTouchState(event.getX(), event.getY(), checkableSpans[0]), 0, 0, Spannable.SPAN_MARK_MARK);
                    Touch.onTouchEvent(textView, buffer, event);
                    return true;
                }
                return super.onTouchEvent(textView, buffer, event);
            }
            case MotionEvent.ACTION_MOVE: {
                boolean result = super.onTouchEvent(textView, buffer, event);
                CheckboxTouchState[] states = buffer.getSpans(0, buffer.length(), CheckboxTouchState.class);
                if (states.length > 0) {
                    CheckboxTouchState state = states[0];
                    if (Math.hypot(event.getX() - state.x, event.getY() - state.y) > touchSlop) {
                        buffer.removeSpan(state);
                        textView.setFocusableInTouchMode(true);
                    }
                }
                return result;
            }
            case MotionEvent.ACTION_UP: {
                CheckboxTouchState[] states = buffer.getSpans(0, buffer.length(), CheckboxTouchState.class);
                if (states.length > 0) {
                    CheckboxTouchState state = states[0];
                    buffer.removeSpan(state);
                    textView.setFocusableInTouchMode(true);
                    if (Math.hypot(event.getX() - state.x, event.getY() - state.y) <= touchSlop) {
                        state.targetSpan.onClick(textView);
                        Touch.onTouchEvent(textView, buffer, event);
                        return true;
                    }
                }
                return super.onTouchEvent(textView, buffer, event);
            }
            case MotionEvent.ACTION_CANCEL: {
                CheckboxTouchState[] states = buffer.getSpans(0, buffer.length(), CheckboxTouchState.class);
                for (CheckboxTouchState state : states) {
                    buffer.removeSpan(state);
                }
                textView.setFocusableInTouchMode(true);
                return super.onTouchEvent(textView, buffer, event);
            }
            default:
                return super.onTouchEvent(textView, buffer, event);
        }
    }
}
