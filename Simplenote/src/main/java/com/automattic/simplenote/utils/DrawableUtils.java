package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

@SuppressWarnings("unused")
public class DrawableUtils {
    public static void setMenuItemAlpha(MenuItem menuItem, @FloatRange(from=0,to=1) double alpha) {
        Drawable drawable = menuItem.getIcon();

        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable).mutate();
            drawable.setAlpha((int) (alpha * 255));  // 255 is 100% opacity.
        }
    }

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

    public static int getColor(Context context, @AttrRes int tintColorAttribute) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(tintColorAttribute, typedValue, true);
        return typedValue.data;
    }

    public static void startAnimatedVectorDrawable(Drawable drawable) {
        if (drawable instanceof AnimatedVectorDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ((AnimatedVectorDrawable) drawable).start();
        } else if (drawable instanceof AnimatedVectorDrawableCompat) {
            ((AnimatedVectorDrawableCompat) drawable).start();
        }
    }

    public static void tintMenuWithAttribute(Context context, Menu menu, @AttrRes int tintColorAttribute) {
        @ColorInt int color = getColor(context, tintColorAttribute);
        DrawableUtils.tintMenu(menu, color);
    }

    public static void tintMenu(Menu menu, @ColorInt int color) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            DrawableUtils.tintMenuItem(item, color);
        }
    }

    public static void tintMenuWithResource(Context context, Menu menu, @ColorRes int colorRes) {
        @ColorInt int color = ContextCompat.getColor(context, colorRes);
        DrawableUtils.tintMenu(menu, color);
    }

    public static void tintMenuItem(MenuItem menuItem, @ColorInt int color) {
        Drawable tinted = DrawableUtils.tintDrawable(menuItem.getIcon(), color);
        menuItem.setIcon(tinted);
    }

    public static void tintMenuItemWithResource(Context context, MenuItem menuItem, @ColorRes int colorRes) {
        @ColorInt int color = ContextCompat.getColor(context, colorRes);
        DrawableUtils.tintMenuItem(menuItem, color);
    }

    public static void tintMenuItemWithAttribute(Context context, MenuItem menuItem, @AttrRes int tintColorAttribute) {
        @ColorInt int color = getColor(context, tintColorAttribute);
        DrawableUtils.tintMenuItem(menuItem, color);
    }
}
