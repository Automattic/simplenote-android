package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.net.Uri;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.automattic.simplenote.R;

import java.util.List;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

public class ThemeUtils {
    public static final int STYLE_CLASSIC = 1;
    public static final int STYLE_DEFAULT = 0;

    private static final String PREFERENCES_URI_AUTHORITY = "preferences";
    private static final String URI_SEGMENT_THEME = "theme";
    private static final int THEME_AUTO = 2;
    private static final int THEME_DARK = 1;
    private static final int THEME_LIGHT = 0;
    private static final int THEME_SYSTEM = 3;

    public static void setTheme(Activity activity) {
            // if we have a data uri that sets the theme let's do it here
        Uri data = activity.getIntent().getData();
        if (data != null) {
            if (data.getAuthority().equals(PREFERENCES_URI_AUTHORITY)) {
                List<String> segments = data.getPathSegments();

                // check if we have reached /preferences/theme
                if (segments.size() > 0 && segments.get(0).equals(URI_SEGMENT_THEME)) {

                    // activate the theme preference
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(PrefUtils.PREF_THEME_MODIFIED, true);

                    editor.apply();
                }
            }
        }

        switch (PrefUtils.getIntPref(activity, PrefUtils.PREF_THEME, THEME_LIGHT)) {
            case THEME_AUTO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_TIME);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static boolean isLightTheme(Context context) {
        return (context.getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) != UI_MODE_NIGHT_YES;
    }

    /*
     * returns the optimal pixel width to use for the menu drawer based on:
     * http://www.google.com/design/spec/layout/structure.html#structure-side-nav
     * http://www.google.com/design/spec/patterns/navigation-drawer.html
     * http://android-developers.blogspot.co.uk/2014/10/material-design-on-android-checklist.html
     * https://medium.com/sebs-top-tips/material-navigation-drawer-sizing-558aea1ad266
     */
    public static int getOptimalDrawerWidth(Context context) {
        Point displaySize = DisplayUtils.getDisplayPixelSize(context);
        int appBarHeight = DisplayUtils.getActionBarHeight(context);
        int drawerWidth = Math.min(displaySize.x, displaySize.y) - appBarHeight;
        int maxDp = (DisplayUtils.isXLarge(context) ? 400 : 320);
        int maxPx = DisplayUtils.dpToPx(context, maxDp);
        return Math.min(drawerWidth, maxPx);
    }

    public static int getThemeTextColorId(Context context) {
        if (context == null) {
            return 0;
        }

        int[] attrs = {R.attr.noteEditorTextColor};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int textColorId = ta.getResourceId(0, android.R.color.black);
        ta.recycle();

        return textColorId;
    }

    public static int getColorFromAttribute(@NonNull Context context, @AttrRes int attribute) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attribute});
        int colorResId = typedArray.getResourceId(0, android.R.color.black);
        typedArray.recycle();
        return context.getColor(colorResId);
    }

    public static String getCssFromStyle(Context context) {
        boolean isLight = isLightTheme(context);

        switch (PrefUtils.getStyleIndex(context)) {
            case STYLE_CLASSIC:
            case STYLE_DEFAULT:
            default:
                return isLight ? "light.css" : "dark.css";
        }
    }

    public static int getStyle(Context context) {
        if (PrefUtils.getStyleName(context).isEmpty() || !PrefUtils.isPremium(context)) {
            return R.style.Style_Default;
        } else {
            switch (PrefUtils.getStyleIndex(context)) {
                case STYLE_CLASSIC:
                    return R.style.Style_Classic;
                case STYLE_DEFAULT:
                default:
                    return R.style.Style_Default;
            }
        }
    }
}
