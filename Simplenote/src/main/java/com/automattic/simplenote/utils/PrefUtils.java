package com.automattic.simplenote.utils;

/*
 *  misc. routines for Simplenote preferences
 *  added 01-Apr-2013 by Nick Bradbury
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.automattic.simplenote.Simplenote;

public class PrefUtils {

	// key names for various preferences - note that these same key names are hard-coded in Preferences.xml
	public static final String PREF_SORT_ORDER 		  = "pref_key_sort_order";		// integer, determines note sort order
	public static final String PREF_NUM_PREVIEW_LINES = "pref_key_preview_lines";	// integer, determines # of preview lines
	public static final String PREF_SHOW_DATES 		  = "pref_key_show_dates";		// boolean, determines whether dates are shown
	
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
			Log.e(Simplenote.TAG, e.getMessage(), e);
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
			Log.e(Simplenote.TAG, e.getMessage(), e);
			return defaultValue;
		}
	}
}
