package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.automattic.simplenote.R;

import java.util.List;

public class ThemeUtils {

    // theme constants
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    @SuppressWarnings("unused")
    public static final int THEME_AUTO = 2;
    private static final String PREFERENCES_URI_AUTHORITY = "preferences";
    private static final String URI_SEGMENT_THEME = "theme";
    private static String THEME_CHANGED_EXTRA = "themeChanged";

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

        int theme = PrefUtils.getIntPref(activity, PrefUtils.PREF_THEME, THEME_LIGHT);
        if (theme == THEME_LIGHT)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else if (theme == THEME_DARK)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    public static boolean isLightTheme(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        return uiMode != Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean themeWasChanged(Intent intent) {
        return intent != null && intent.getBooleanExtra(THEME_CHANGED_EXTRA, false);
    }

    public static Intent makeThemeChangeIntent() {
        Intent intent = new Intent();
        intent.putExtra(THEME_CHANGED_EXTRA, true);
        return intent;
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
}