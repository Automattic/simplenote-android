package com.automattic.simplenote.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import java.util.Hashtable;

public class TypefaceCache {

    public static final String TYPEFACE_NAME_ROBOTO_REGULAR = "Roboto-Regular.ttf";
    public static final String TYPEFACE_NAME_ROBOTO_MEDIUM = "Roboto-Medium.ttf";
    public static final String TYPEFACE_NAME_ROBOTO_LIGHT = "Roboto-Light.ttf";

    private static final Hashtable<String, Typeface> mTypefaceCache = new Hashtable<>();

    public static Typeface getTypeface(Context context, String typefaceName) {
        if (context == null || typefaceName == null) {
            return null;
        }

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
    protected static void setCustomTypeface(Context context, TextView view, String typefaceName) {
        if (context == null || view == null || typefaceName == null) {
            return;
        }

        // skip at design-time
        if (view.isInEditMode()) return;


        Typeface typeface = getTypeface(context, typefaceName);
        if (typeface != null) {
            view.setTypeface(typeface);
        }
    }
}
