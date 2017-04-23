package com.automattic.simplenote.utils;

/*
 *  misc. routines for Simplenote preferences
 *  added 01-Apr-2013 by Nick Bradbury
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.automattic.simplenote.BuildConfig;

@SuppressWarnings("unused")
public class PrefUtils {

    // key names for various preferences - note that these same key names are hard-coded in Preferences.xml

    // integer, determines note sort order
    public static final String PREF_SORT_ORDER = "pref_key_sort_order";

    // boolean, determines # of preview lines
    public static final String PREF_CONDENSED_LIST = "pref_key_condensed_note_list";

    // boolean, determines whether dates are shown
    public static final String PREF_SHOW_DATES = "pref_key_show_dates";

    // int, preferred font size
    public static final String PREF_FONT_SIZE = "pref_key_font_size";

    // boolean, set on first launch
    public static final String PREF_FIRST_LAUNCH = "pref_key_first_launch";

    // boolean, set to require an account to access the app
    public static final String PREF_ACCOUNT_REQUIRED = "pref_key_account_required";

    // boolean, set on when user taps to just try the app in the welcome view
    public static final String PREF_APP_TRIAL = "pref_key_app_trial";

    // boolean, allow notes to preview markdown
    public static final String PREF_MARKDOWN_ENABLED = "pref_key_markdown_enabled";

    // string. determines theme to use
    public static final String PREF_THEME = "pref_key_theme";

    // boolean, determines if the theme was ever changed
    public static final String PREF_THEME_MODIFIED = "pref_theme_modified";

    // string, the cryptography passphrase (can be easily seen in rooted devices)
    public static final String PREF_CRYPTO_PASS_PHRASE = "pref_crypto_pass_phrase";

    private static SharedPreferences getPrefs(Context context) {
        // todo we should cache this instance instead of getting it fresh each time, for better performance.
        // see: http://stackoverflow.com/a/4371883/4718652
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
            return HtmlCompat.fromHtml(info);
        }

        return BuildConfig.VERSION_NAME;

    }

}
