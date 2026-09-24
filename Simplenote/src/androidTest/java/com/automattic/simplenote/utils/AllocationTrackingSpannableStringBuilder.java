package com.automattic.simplenote.utils;

import android.text.SpannableStringBuilder;

public class AllocationTrackingSpannableStringBuilder extends SpannableStringBuilder {
    private boolean mTrackingEnabled;
    private int mAllocationCount;

    public AllocationTrackingSpannableStringBuilder() {
        super();
    }

    public AllocationTrackingSpannableStringBuilder(CharSequence text) {
        super(text);
    }

    public AllocationTrackingSpannableStringBuilder(CharSequence text, int start, int end) {
        super(text, start, end);
    }

    public void setTrackingEnabled(boolean enabled) {
        mTrackingEnabled = enabled;
    }

    public int getAllocationCount() {
        return mAllocationCount;
    }

    public void resetAllocationCount() {
        mAllocationCount = 0;
    }

    @Override
    public String toString() {
        if (mTrackingEnabled) {
            mAllocationCount++;
            throw new AssertionError("Unexpected full-text toString() allocation on hot path");
        }
        return super.toString();
    }
}
