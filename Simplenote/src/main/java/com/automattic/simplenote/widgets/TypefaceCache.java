package com.automattic.simplenote.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.Hashtable;

public class TypefaceCache {

    private static final Hashtable<String, Typeface> mTypefaceCache = new Hashtable<String, Typeface>();

    public static Typeface getTypeface(Context context) {
        if (context == null)
            return null;

        // note that the "light" variation doesn't support bold or bold-italic
        final String typefaceName = "SourceSansPro-Regular.ttf";

        if (!mTypefaceCache.containsKey(typefaceName)) {
            Typeface typeface = Typeface.createFromAsset(context.getApplicationContext().getAssets(), "fonts/"
                    + typefaceName);
            if (typeface != null) {
                mTypefaceCache.put(typefaceName, typeface);
            }
        }

        return mTypefaceCache.get(typefaceName);
    }

    /*
     * sets the typeface for a TextView (or TextView descendant such as EditText or Button) based on
     * the passed attributes, defaults to normal typeface
     */
    protected static void setCustomTypeface(Context context, TextView view, AttributeSet attrs) {
        if (context == null || view == null)
            return;

        // skip at design-time
        if (view.isInEditMode())
            return;


        Typeface typeface = getTypeface(context);
        if (typeface != null) {
            view.setTypeface(typeface);
        }
    }
}
