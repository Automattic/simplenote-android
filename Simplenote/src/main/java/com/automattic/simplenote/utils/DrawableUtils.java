package com.automattic.simplenote.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

/**
 * Created by Ondrej Ruttkay on 18/03/2016.
 */
public class DrawableUtils {

    public static Drawable tintDrawable(Context context, @DrawableRes int drawableResource, @ColorRes int color) {
        Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, drawableResource)).mutate();
        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, color));
        return drawable;
    }
}
