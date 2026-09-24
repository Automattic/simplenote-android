package com.automattic.simplenote.utils;

import android.content.Context;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.automattic.simplenote.widgets.CheckableSpan;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SimplenoteMovementMethodTest {
    private Context mContext;
    private SimplenoteMovementMethod mMovementMethod;

    private static class TestCheckableSpan extends CheckableSpan {
        private final AtomicBoolean mClicked = new AtomicBoolean(false);

        TestCheckableSpan() {
            super();
            setChecked(false);
        }

        @Override
        public void onClick(View widget) {
            mClicked.set(true);
        }

        boolean isClicked() {
            return mClicked.get();
        }
    }

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mMovementMethod = SimplenoteMovementMethod.getInstance();
    }

    private TextView createLaidOutTextView(Spannable spannable) {
        TextView textView = new TextView(mContext);
        textView.setText(spannable);
        textView.setMovementMethod(mMovementMethod);

        int widthSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY);
        textView.measure(widthSpec, heightSpec);
        textView.layout(0, 0, 800, 1200);

        return textView;
    }

    @Test
    public void testNullLayoutSafety() {
        TextView textView = new TextView(mContext);
        SpannableString spannable = new SpannableString("Text without layout");
        long time = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 50f, 50f, 0);

        boolean result = mMovementMethod.onTouchEvent(textView, spannable, downEvent);
        downEvent.recycle();
        // Should not throw NullPointerException
        assertTrue("onTouchEvent should return safely when layout is null", result || !result);
    }

    @Test
    public void testTapWithinTouchSlop() {
        SpannableString text = new SpannableString("- [ ] First item\nSecond line");
        TestCheckableSpan span = new TestCheckableSpan();
        text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        Layout layout = textView.getLayout();

        float x = layout.getPrimaryHorizontal(0) + textView.getTotalPaddingLeft();
        float y = layout.getLineTop(0) + textView.getTotalPaddingTop() + 10f;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(1, states.length);

        MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, x, y, 0);
        boolean upConsumed = mMovementMethod.onTouchEvent(textView, text, upEvent);
        upEvent.recycle();

        assertTrue(upConsumed);
        assertTrue("Checkbox onClick should be called on tap", span.isClicked());
        states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(0, states.length);
        assertTrue(textView.isFocusableInTouchMode());
    }

    @Test
    public void testTapAdjacentWhitespaceTogglesCheckbox() {
        SpannableString text = new SpannableString("- [ ] First item\nSecond line");
        TestCheckableSpan span = new TestCheckableSpan();
        text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        Layout layout = textView.getLayout();

        // Target lineStart + 1 (adjacent space)
        float x = layout.getPrimaryHorizontal(1) + textView.getTotalPaddingLeft();
        float y = layout.getLineTop(0) + textView.getTotalPaddingTop() + 10f;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(1, states.length);

        MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, upEvent);
        upEvent.recycle();

        assertTrue("Adjacent whitespace tap should toggle checkbox", span.isClicked());
        states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(0, states.length);
    }

    @Test
    public void testTapWithinTouchSlopWhenScrolled() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Line ").append(i).append("\n");
        }
        sb.append("- [ ] Checkbox line 50\n");
        SpannableString text = new SpannableString(sb.toString());

        TestCheckableSpan span = new TestCheckableSpan();
        int targetSpanIndex = sb.indexOf("- [ ]");
        text.setSpan(span, targetSpanIndex, targetSpanIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        Layout layout = textView.getLayout();
        int line = layout.getLineForOffset(targetSpanIndex);
        int lineTop = layout.getLineTop(line);

        // Scroll the view so the line is in view
        textView.scrollTo(0, lineTop);

        // Screen touch coordinates (View coordinates)
        float touchX = layout.getPrimaryHorizontal(targetSpanIndex) + textView.getTotalPaddingLeft();
        float touchY = 20f; // Near top of visible view

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, touchX, touchY, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(1, states.length);

        MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, touchX, touchY, 0);
        mMovementMethod.onTouchEvent(textView, text, upEvent);
        upEvent.recycle();

        assertTrue("Checkbox tap while scrolled should succeed", span.isClicked());
    }

    @Test
    public void testDragBeyondTouchSlop() {
        SpannableString text = new SpannableString("- [ ] First item\nSecond line");
        TestCheckableSpan span = new TestCheckableSpan();
        text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        Layout layout = textView.getLayout();

        float x = layout.getPrimaryHorizontal(0) + textView.getTotalPaddingLeft();
        float y = layout.getLineTop(0) + textView.getTotalPaddingTop() + 10f;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        int touchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        MotionEvent moveEvent = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_MOVE, x, y + touchSlop + 50f, 0);
        mMovementMethod.onTouchEvent(textView, text, moveEvent);
        moveEvent.recycle();

        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals("CheckboxTouchState should be removed when dragging beyond slop", 0, states.length);
        assertTrue(textView.isFocusableInTouchMode());

        MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, x, y + touchSlop + 50f, 0);
        mMovementMethod.onTouchEvent(textView, text, upEvent);
        upEvent.recycle();

        assertFalse("Checkbox should NOT be clicked after drag", span.isClicked());
    }

    @Test
    public void testDragBeyondTextHeightCleansUpState() {
        SpannableString text = new SpannableString("- [ ] First item");
        TestCheckableSpan span = new TestCheckableSpan();
        text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        Layout layout = textView.getLayout();

        float x = layout.getPrimaryHorizontal(0) + textView.getTotalPaddingLeft();
        float y = layout.getLineTop(0) + textView.getTotalPaddingTop() + 5f;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        // Move far below text height
        float farY = layout.getHeight() + 200f;
        MotionEvent moveEvent = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_MOVE, x, farY, 0);
        mMovementMethod.onTouchEvent(textView, text, moveEvent);
        moveEvent.recycle();

        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(0, states.length);
        assertTrue(textView.isFocusableInTouchMode());
    }

    @Test
    public void testTapBeyondTextHeightDelegatesWithoutCheckboxToggle() {
        SpannableString text = new SpannableString("- [ ] First item");
        TestCheckableSpan span = new TestCheckableSpan();
        text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        Layout layout = textView.getLayout();

        float x = 100f;
        float y = layout.getHeight() + 300f; // well below text height

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals("No CheckboxTouchState should be attached when tapping below text height", 0, states.length);

        MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, upEvent);
        upEvent.recycle();

        assertFalse("Checkbox should NOT be clicked when tapping below text height", span.isClicked());
    }

    @Test
    public void testGestureCancellation() {
        SpannableString text = new SpannableString("- [ ] First item");
        TestCheckableSpan span = new TestCheckableSpan();
        text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        Layout layout = textView.getLayout();

        float x = layout.getPrimaryHorizontal(0) + textView.getTotalPaddingLeft();
        float y = layout.getLineTop(0) + textView.getTotalPaddingTop() + 5f;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(1, states.length);

        MotionEvent cancelEvent = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_CANCEL, x, y, 0);
        mMovementMethod.onTouchEvent(textView, text, cancelEvent);
        cancelEvent.recycle();

        states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(0, states.length);
        assertTrue(textView.isFocusableInTouchMode());
        assertFalse(span.isClicked());
    }

    @Test
    public void testInterruptedGestureFocusRestoration() {
        SpannableString text = new SpannableString("- [ ] First item\nSecond line");
        TestCheckableSpan span = new TestCheckableSpan();
        text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = createLaidOutTextView(text);
        textView.setFocusableInTouchMode(false);
        text.setSpan(new SimplenoteMovementMethod.CheckboxTouchState(10f, 10f, span), 0, 0, Spannable.SPAN_MARK_MARK);

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        mMovementMethod.onTouchEvent(textView, text, downEvent);
        downEvent.recycle();

        assertTrue("FocusableInTouchMode should be restored on ACTION_DOWN", textView.isFocusableInTouchMode());
        SimplenoteMovementMethod.CheckboxTouchState[] states = text.getSpans(0, text.length(), SimplenoteMovementMethod.CheckboxTouchState.class);
        assertEquals(0, states.length);
    }
}
