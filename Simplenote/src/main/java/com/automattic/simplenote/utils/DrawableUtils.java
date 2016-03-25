package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by Ondrej Ruttkay on 18/03/2016.
 */
public class DrawableUtils {

    public static Drawable tintDrawableWithResource(Context context, @DrawableRes int drawableRes, @ColorRes int colorRes) {
        return tintDrawableWithResource(context, ContextCompat.getDrawable(context, drawableRes), colorRes);
    }

    public static Drawable tintDrawable(Context context, @DrawableRes int drawableRes, @ColorInt int color) {
        return tintDrawable(ContextCompat.getDrawable(context, drawableRes), color);
    }

    public static Drawable tintDrawableWithResource(Context context, Drawable drawable, @ColorRes int colorRes) {
        @ColorInt int tint = ContextCompat.getColor(context, colorRes);
        return tintDrawable(drawable, tint);
    }

    public static Drawable tintDrawable(Drawable drawable, @ColorInt int color) {
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable).mutate();
            DrawableCompat.setTint(drawable, color);
        }
        return drawable;
    }

    public static Drawable tintDrawableWithAttribute(Context context, @DrawableRes int drawableRes,
                                                     @AttrRes int tintColorAttribute) {
        @ColorInt int color = getColor(context, tintColorAttribute);
        return tintDrawable(context, drawableRes, color);
    }

    private static int getColor(Context context, @AttrRes int tintColorAttribute) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(tintColorAttribute, typedValue, true);
        return typedValue.data;
    }

    public static void tintMenuWithAttribute(Context context, Menu menu, @AttrRes int tintColorAttribute) {
        @ColorInt int color = getColor(context, tintColorAttribute);
        DrawableUtils.tintMenu(menu, color);
    }

    public static void tintMenu(Menu menu, @ColorInt int color) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Drawable tinted = DrawableUtils.tintDrawable(item.getIcon(), color);
            item.setIcon(tinted);
        }
    }
}
