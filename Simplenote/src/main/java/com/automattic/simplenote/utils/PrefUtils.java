package com.automattic.simplenote.utils;

/*
 *  misc. routines for Simplenote preferences
 *  added 01-Apr-2013 by Nick Bradbury
 */

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.IntDef;
import androidx.preference.PreferenceManager;

import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.R;
import com.automattic.simplenote.models.Note;
import com.simperium.client.Query;

import java.lang.annotation.Retention;

import static com.automattic.simplenote.models.Note.PINNED_INDEX_NAME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@SuppressWarnings("unused")
public class PrefUtils {

    // key names for various preferences - note that these same key names are hard-coded in Preferences.xml

    // integer, determines note sort order
    public static final String PREF_SORT_ORDER = "pref_key_sort_order";

    // boolean, determines # of preview lines
    public static final String PREF_CONDENSED_LIST = "pref_key_condensed_note_list";

    // boolean, determines whether to sort the tags list alphabetically
    public static final String PREF_SORT_TAGS_ALPHA = "pref_key_sort_tags_alpha";

    // boolean, determines whether dates are shown
    public static final String PREF_SHOW_DATES = "pref_key_show_dates";

    // int, preferred font size
    private static final String PREF_FONT_SIZE = "pref_key_font_size";

    // boolean, determines linkifying content in the editor
    public static final String PREF_DETECT_LINKS = "pref_key_detect_links";

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

    // string. WordPress.com access token
    public static final String PREF_WP_TOKEN = "pref_key_wp_token";

    // boolean. determines if analytics is enabled
    public static final String PREF_ANALYTICS_ENABLED = "pref_key_analytics_enabled";

    // string. json array of sites used to publish to WordPress
    public static final String PREF_WORDPRESS_SITES = "pref_key_wordpress_sites";

    // string. Store notes linked to note widget instances.
    public static final String PREF_NOTE_WIDGET_NOTE = "pref_key_note_widget_";

    public static final int ALPHABETICAL_ASCENDING = 4;
    public static final int ALPHABETICAL_DESCENDING = 5;
    public static final int DATE_CREATED_ASCENDING = 3;
    public static final int DATE_CREATED_DESCENDING = 2;
    public static final int DATE_MODIFIED_ASCENDING = 1;
    public static final int DATE_MODIFIED_DESCENDING = 0;

    @Retention(SOURCE)
    @IntDef({
        DATE_MODIFIED_DESCENDING,
        DATE_MODIFIED_ASCENDING,
        DATE_CREATED_DESCENDING,
        DATE_CREATED_ASCENDING,
        ALPHABETICAL_ASCENDING,
        ALPHABETICAL_DESCENDING
    })
    public @interface Sort {}

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
            return HtmlCompat.fromHtml(info);
        }

        return BuildConfig.VERSION_NAME;
    }

    public static int getFontSize(Context context) {
        int defaultFontSize = 16;
        // Just in case
        if (context == null) {
            return defaultFontSize;
        }

        // Get default value for normal font size (differs based on screen/dpi size)
        defaultFontSize = context.getResources().getInteger(R.integer.default_font_size);

        return getIntPref(context, PREF_FONT_SIZE, defaultFontSize);
    }

    public static void sortNoteQuery(Query<Note> query, Context context, boolean includePinnedOrdering) {
        if (includePinnedOrdering) {
            query.order(PINNED_INDEX_NAME, Query.SortType.DESCENDING);
        }

        switch (PrefUtils.getIntPref(context, PrefUtils.PREF_SORT_ORDER)) {
            case DATE_MODIFIED_DESCENDING:
                query.order(Note.MODIFIED_INDEX_NAME, Query.SortType.DESCENDING);
                break;
            case DATE_MODIFIED_ASCENDING:
                query.order(Note.MODIFIED_INDEX_NAME, Query.SortType.ASCENDING);
                break;
            case DATE_CREATED_DESCENDING:
                query.order(Note.CREATED_INDEX_NAME, Query.SortType.DESCENDING);
                break;
            case DATE_CREATED_ASCENDING:
                query.order(Note.CREATED_INDEX_NAME, Query.SortType.ASCENDING);
                break;
            case ALPHABETICAL_ASCENDING:
                query.order(Note.CONTENT_PROPERTY, Query.SortType.ASCENDING);
                break;
            case ALPHABETICAL_DESCENDING:
                query.order(Note.CONTENT_PROPERTY, Query.SortType.DESCENDING);
                break;
        }
    }
}
