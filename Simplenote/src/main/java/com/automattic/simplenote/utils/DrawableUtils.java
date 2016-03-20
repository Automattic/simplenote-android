package com.automattic.simplenote.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

/**
 * Created by Ondrej Ruttkay on 18/03/2016.
 */
public class DrawableUtils {

    public static Drawable tintDrawable(Context context, @DrawableRes int drawableResource, @ColorRes int color) {
        return tintDrawable(context, ContextCompat.getDrawable(context, drawableResource), color);
    }

    public static Drawable tintDrawable(Context context, Drawable drawable, @ColorRes int color) {
        @ColorInt int tint = ContextCompat.getColor(context, color);
        return tintDrawable(drawable, tint);
    }

    public static Drawable tintDrawable(Drawable drawable, @ColorInt int color) {
        drawable = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }
}
