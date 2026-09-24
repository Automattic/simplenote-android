package com.automattic.simplenote.widgets;

import android.content.Context;
import android.os.SystemClock;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.MultiAutoCompleteTextView;
import android.widget.OverScroller;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.automattic.simplenote.utils.AllocationTrackingSpannableStringBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SimplenoteEditTextTest {
    private Context mContext;
    private SimplenoteEditText mEditText;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mEditText = new SimplenoteEditText(mContext);
    }

    @Test
    public void testBaseClassInheritance() {
        assertTrue("SimplenoteEditText should extend MultiAutoCompleteTextView",
                mEditText instanceof MultiAutoCompleteTextView);
    }

    @Test
    public void testFlingInterruptOnActionDown() {
        OverScroller scroller = new OverScroller(mContext);
        scroller.startScroll(0, 0, 0, 500, 1000);
        assertFalse(scroller.isFinished());

        mEditText.setScrollerForTest(scroller);

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        boolean consumed = mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertTrue("ACTION_DOWN during active fling must be consumed", consumed);
        assertTrue("Scroller must be force-finished", scroller.isFinished());
        assertTrue("isFlingInterrupt must be true", mEditText.isFlingInterrupt());

        // Now test when scroller is finished
        downTime = SystemClock.uptimeMillis();
        downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertFalse("isFlingInterrupt must be false when scroller is finished", mEditText.isFlingInterrupt());
    }

    @Test
    public void testFlingInterruptMoveWithinSlop() {
        OverScroller scroller = new OverScroller(mContext);
        scroller.startScroll(0, 0, 0, 500, 1000);
        mEditText.setScrollerForTest(scroller);

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertTrue(mEditText.isFlingInterrupt());

        int touchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        MotionEvent moveEvent = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_MOVE, 100f, 100f + (touchSlop / 2.0f), 0);
        boolean moveConsumed = mEditText.onTouchEvent(moveEvent);
        moveEvent.recycle();

        assertTrue("ACTION_MOVE within touch slop must be consumed", moveConsumed);
        assertTrue("isFlingInterrupt should remain true within touch slop", mEditText.isFlingInterrupt());
    }

    @Test
    public void testFlingInterruptMoveBeyondSlop() {
        OverScroller scroller = new OverScroller(mContext);
        scroller.startScroll(0, 0, 0, 500, 1000);
        mEditText.setScrollerForTest(scroller);

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertTrue(mEditText.isFlingInterrupt());

        int touchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        MotionEvent moveEvent = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_MOVE, 100f, 100f + touchSlop + 50.0f, 0);
        mEditText.onTouchEvent(moveEvent);
        moveEvent.recycle();

        assertFalse("isFlingInterrupt must be cleared when move exceeds touch slop", mEditText.isFlingInterrupt());
    }

    @Test
    public void testFlingInterruptUpAndCancelRecycleVelocityTracker() {
        OverScroller scroller = new OverScroller(mContext);
        scroller.startScroll(0, 0, 0, 500, 1000);
        mEditText.setScrollerForTest(scroller);

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertTrue(mEditText.isFlingInterrupt());

        MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_UP, 100f, 100f, 0);
        boolean upConsumed = mEditText.onTouchEvent(upEvent);
        upEvent.recycle();

        assertTrue("ACTION_UP during fling interrupt must be consumed", upConsumed);
        assertFalse("isFlingInterrupt must be reset after UP", mEditText.isFlingInterrupt());
        assertFalse("VelocityTracker must be recycled on UP", mEditText.isVelocityTrackerActive());

        // Test CANCEL branch
        scroller = new OverScroller(mContext);
        scroller.startScroll(0, 0, 0, 500, 1000);
        mEditText.setScrollerForTest(scroller);

        downTime = SystemClock.uptimeMillis();
        downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertTrue(mEditText.isFlingInterrupt());

        MotionEvent cancelEvent = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_CANCEL, 100f, 100f, 0);
        boolean cancelConsumed = mEditText.onTouchEvent(cancelEvent);
        cancelEvent.recycle();

        assertTrue("ACTION_CANCEL during fling interrupt must be consumed", cancelConsumed);
        assertFalse("isFlingInterrupt must be reset after CANCEL", mEditText.isFlingInterrupt());
        assertFalse("VelocityTracker must be recycled on CANCEL", mEditText.isVelocityTrackerActive());
    }

    @Test
    public void testMultiTouchPointerEventsDuringFlingInterrupt() {
        OverScroller scroller = new OverScroller(mContext);
        scroller.startScroll(0, 0, 0, 500, 1000);
        mEditText.setScrollerForTest(scroller);

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertTrue(mEditText.isFlingInterrupt());

        MotionEvent pointerDown = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_POINTER_DOWN, 120f, 120f, 0);
        boolean pointerDownConsumed = mEditText.onTouchEvent(pointerDown);
        pointerDown.recycle();
        assertTrue("ACTION_POINTER_DOWN during fling interrupt must be consumed", pointerDownConsumed);

        MotionEvent pointerUp = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_POINTER_UP, 120f, 120f, 0);
        boolean pointerUpConsumed = mEditText.onTouchEvent(pointerUp);
        pointerUp.recycle();
        assertTrue("ACTION_POINTER_UP during fling interrupt must be consumed", pointerUpConsumed);
    }

    @Test
    public void testOnDetachedFromWindowCleansUpState() {
        OverScroller scroller = new OverScroller(mContext);
        scroller.startScroll(0, 0, 0, 500, 1000);
        mEditText.setScrollerForTest(scroller);

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        mEditText.onTouchEvent(downEvent);
        downEvent.recycle();

        assertTrue(mEditText.isFlingInterrupt());
        assertTrue(mEditText.isVelocityTrackerActive());

        mEditText.onDetachedFromWindow();

        assertFalse("isFlingInterrupt must be reset on detached", mEditText.isFlingInterrupt());
        assertFalse("VelocityTracker must be recycled on detached", mEditText.isVelocityTrackerActive());
        assertTrue("Scroller must be finished on detached", scroller.isFinished());
    }

    @Test
    public void testEnoughToFilterScaleInvariant() {
        StringBuilder longTitle = new StringBuilder("[");
        for (int i = 0; i < 300; i++) {
            longTitle.append("a");
        }
        longTitle.append("](simplenote://note/1234567890abcdef)\nNext line text");
        mEditText.setText(longTitle.toString());

        // Set cursor inside the link title (>200 chars)
        mEditText.setSelection(150);
        assertFalse("enoughToFilter should return false when editing internote link title", mEditText.enoughToFilter());

        // Test out of bounds cursor
        mEditText.setSelection(0);
        assertFalse(mEditText.enoughToFilter());
    }

    @Test
    public void testProcessChecklistsInPlaceBounds() {
        final AllocationTrackingSpannableStringBuilder[] trackingHolder = new AllocationTrackingSpannableStringBuilder[1];

        mEditText.setEditableFactory(new Editable.Factory() {
            @Override
            public Editable newEditable(CharSequence source) {
                AllocationTrackingSpannableStringBuilder builder = new AllocationTrackingSpannableStringBuilder(source);
                trackingHolder[0] = builder;
                return builder;
            }
        });

        mEditText.setText("- [ ] Item 1\n- [ ] Item 2\nRegular line 3\n");
        AllocationTrackingSpannableStringBuilder builder = trackingHolder[0];

        builder.resetAllocationCount();
        builder.setTrackingEnabled(true);

        // Process line 3 (regular text line without checklist markers)
        mEditText.processChecklists(26, 14);

        builder.setTrackingEnabled(false);
        assertEquals("processChecklists must not invoke full-text toString() on hot path",
                0, builder.getAllocationCount());
    }
}
