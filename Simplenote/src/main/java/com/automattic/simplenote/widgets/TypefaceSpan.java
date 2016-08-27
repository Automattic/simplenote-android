package com.automattic.simplenote.widgets;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

/**
 * A custom span for setting the action bar title font.
 * Created by dan on 8/13/13.
 */
public class TypefaceSpan extends MetricAffectingSpan {

    private Typeface mTypeface;

    /**
     * Load the {@link Typeface} and apply to a {@link Spannable}.
     */
    public TypefaceSpan(Context context) {
        mTypeface = TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR);
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        p.setTypeface(mTypeface);

        // Note: This flag is required for proper typeface rendering
        p.setFlags(p.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setTypeface(mTypeface);

        // Note: This flag is required for proper typeface rendering
        tp.setFlags(tp.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }
}
