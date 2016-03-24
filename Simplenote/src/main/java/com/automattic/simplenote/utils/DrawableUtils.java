package com.automattic.simplenote.utils;

import android.app.Activity;
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

    public static void tintMenu(Activity activity, Menu menu, @AttrRes int tintColorAttribute) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        theme.resolveAttribute(tintColorAttribute, typedValue, true);
        @ColorInt int color = typedValue.data;
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
