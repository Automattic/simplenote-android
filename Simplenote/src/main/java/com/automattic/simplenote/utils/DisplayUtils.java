package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

public class DisplayUtils {
    private DisplayUtils() {
        throw new AssertionError();
    }

    public static boolean isLandscape(Context context) {
        return context != null && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static Point getDisplayPixelSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static int dpToPx(Context context, int dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
        return (int) px;
    }

    public static boolean isLarge(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isXLarge(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static boolean isLargeScreen(Context context) {
        return isLarge(context) || isXLarge(context);
    }

    public static boolean isLargeScreenLandscape(Context context) {
        return isLargeScreen(context) && isLandscape(context);
    }

    /**
     * returns the height of the ActionBar if one is enabled - supports both the native ActionBar
     * and ActionBarSherlock - http://stackoverflow.com/a/15476793/1673548
     */
    public static int getActionBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        TypedValue tv = new TypedValue();
        if (context.getTheme() != null
                && context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }

        // if we get this far, it's because the device doesn't support an ActionBar,
        // so return the standard ActionBar height (48dp)
        return dpToPx(context, 48);
    }
}

