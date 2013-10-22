package com.automattic.simplenote.utils;

import com.automattic.simplenote.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.List;

public class ThemeUtils {

    // theme constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static String THEME_CHANGED_EXTRA = "themeChanged";

    static public final String PREFERENCES_URI_AUTHORITY = "preferences";

    static public final String URI_SEGMENT_THEME = "theme";

    public static void setTheme(Activity activity) {

        // if we have a data uri that sets the theme let's do it here
        Uri data = activity.getIntent().getData();
        if (data != null) {
            if (data.getAuthority().equals(PREFERENCES_URI_AUTHORITY)) {
                List<String> segments = data.getPathSegments();

                // check if we have reched /preferences/theme
                if (segments.size() > 0 && segments.get(0).equals(URI_SEGMENT_THEME)) {

                    // activate the theme preference
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(PrefUtils.PREF_THEME_MODIFIED, true);

                    editor.commit();

                }
            }
        }

        int theme = PrefUtils.getIntPref(activity, PrefUtils.PREF_THEME, THEME_LIGHT);
        if (theme == THEME_LIGHT)
            activity.setTheme(R.style.Theme_Simplestyle);
        else
            activity.setTheme(R.style.Theme_Simplestyle_Dark);
    }

    public static boolean themeWasChanged(Intent intent) {
        return intent != null && intent.getBooleanExtra(THEME_CHANGED_EXTRA, false);
    }

    public static Intent makeThemeChangeIntent() {
        Intent intent = new Intent();
        intent.putExtra(THEME_CHANGED_EXTRA, true);
        return intent;
    }


}