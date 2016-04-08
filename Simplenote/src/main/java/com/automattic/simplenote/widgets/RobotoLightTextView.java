package com.automattic.simplenote.widgets;

import android.content.Context;
import android.util.AttributeSet;

/**
 * custom TextView used in layouts - enables keeping custom typeface handling in one place (so we
 * avoid having to set the typeface for every single TextView in every single activity)
 */
public class RobotoLightTextView extends TintedTextView {
    public RobotoLightTextView(Context context) {
        super(context);
        TypefaceCache.setCustomTypeface(context, this, TypefaceCache.TYPEFACE_NAME_ROBOTO_LIGHT);
    }

    public RobotoLightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, TypefaceCache.TYPEFACE_NAME_ROBOTO_LIGHT);
    }

    public RobotoLightTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, TypefaceCache.TYPEFACE_NAME_ROBOTO_LIGHT);
    }
}
