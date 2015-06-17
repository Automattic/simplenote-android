package com.automattic.simplenote.utils;

/*
 *  misc. routines for Simplenote preferences
 *  added 01-Apr-2013 by Nick Bradbury
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;

import com.automattic.simplenote.BuildConfig;

public class PrefUtils {

    // key names for various preferences - note that these same key names are hard-coded in Preferences.xml

    // integer, determines note sort order
    public static final String PREF_SORT_ORDER = "pref_key_sort_order";

    // boolean, determines # of preview lines
    public static final String PREF_CONDENSED_LIST = "pref_key_condensed_note_list";

    // boolean, determines whether dates are shown
    public static final String PREF_SHOW_DATES = "pref_key_show_dates";

    // int, preferred font size
    public static final String PREF_FONT_SIZE      = "pref_key_font_size";

    // boolean, set on first launch
    public static final String PREF_FIRST_LAUNCH = "pref_key_first_launch";

    // boolean, set on when user taps to just try the app in the welcome view
    public static final String PREF_APP_TRIAL = "pref_key_app_trial";

    // string. determines theme to use
    public static final String PREF_THEME = "pref_key_theme";

    // boolean, determines if the theme was ever changed
    public static final String PREF_THEME_MODIFIED = "pref_theme_modified";


    /**
     * if this preference exists when the main activity is started the command stored as the
     * value will be executed.  The command will be a {@link
     * com.automattic.simplenote.ActivityCommand} enumeration/serilizable.
     */
    public static final String PREF_ACTIVITY_COMMAND = "pref_activity_command";

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getStringPref(Context context, String prefKey) {
        return getStringPref(context, prefKey, "");
    }

    public static String getStringPref(Context context, String prefKey, String defaultValue) {
        try {
            return getPrefs(context).getString(prefKey, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public static int getIntPref(Context context, String prefKey) {
        return getIntPref(context, prefKey, 0);
    }

    public static int getIntPref(Context context, String prefKey, int defaultValue) {
        // read as string preference, then convert to int
        String strPref = getStringPref(context, prefKey, Integer.toString(defaultValue));
        return StrUtils.strToInt(strPref, defaultValue);
    }

    public static boolean getBoolPref(Context context, String prefKey) {
        return getBoolPref(context, prefKey, false);
    }

    public static boolean getBoolPref(Context context, String prefKey, boolean defaultValue) {
        try {
            return getPrefs(context).getBoolean(prefKey, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public static CharSequence versionInfo() {

        if (BuildConfig.DEBUG) {
            String info = "<strong>" + BuildConfig.VERSION_NAME + "</strong> " +
                    BuildConfig.BUILD_TYPE + " (Build " + BuildConfig.VERSION_CODE + ")" +
                    "\n<em>" + BuildConfig.BUILD_HASH + "</em>";
            return Html.fromHtml(info);
        }

        return BuildConfig.VERSION_NAME;

    }

    public static void removePref(Context context, String prefKey){
        SharedPreferences prefs = getPrefs(context);

        if (!prefs.contains(prefKey)){
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(prefKey);
        editor.apply();
    }


}
