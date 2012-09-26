package com.simperium.simplenote;

import android.app.*;
import android.content.*;

/*
 * this class is a singleton class to encapsulate storing and retrieving preferences
 */

public class Preferences 
{
//	public final static int LockOrientation = 5;
	public final static int WebSyncing = 5;
	public final static int ShowDates = 6;
	public final static int DetectLinks = 7;
	protected static Preferences singletonInstance = null;
    protected static SharedPreferences settings = null;
	protected static String PREFS_NAME = "LoginPrefs";
	protected static String EMAIL = "EMAIL";
	protected static String PASSWORD = "PASSWORD";
	protected static String NUM_ROWS = "NUM_ROWS";
	protected static String SORT_ORDER = "SORT_ORDER";
	protected static String LOCK_ORIENTATION = "LOCK_ORIENTATION";
	protected static String WEB_SYNC = "WEB_SYNC";
	protected static String SHOW_DATES = "SHOW_DATES";
	protected static String DETECT_LINKS = "DETECT_LINKS";
	
	protected static SharedPreferences.Editor editor = null;
	protected static String email, password;
	protected static int numLines, sortOrder;
	protected static boolean lockOrientation, webSync, showDates, detectLinks;
	
	
	public static boolean isOptionChecked(int option)
	{
		boolean answer = false;
		switch(option)
		{
/*		case LockOrientation: // lock orientation
			answer = isLockOrientation();
			break;
*/		case WebSyncing: // web syncing
			answer = isWebSync();
			break;
		case ShowDates: // show dates
			answer = isShowDates();
			break;
/*		case DetectLinks: // detect links
			answer = isDetectLinks();
			break;
*/		}
		return answer;
	}
	
	public static boolean isLockOrientation() 
	{
		return Preferences.lockOrientation;
	}

	public static void setLockOrientation(boolean lockOrientation) 
	{
		savePreference(LOCK_ORIENTATION,  Preferences.lockOrientation = lockOrientation);
	}

	public static boolean isWebSync() 
	{
		return Preferences.webSync;
	}

	public static void setWebSync(boolean webSync) 
	{
		savePreference(WEB_SYNC, Preferences.webSync = webSync);
	}

	public static boolean isShowDates() 
	{
		return Preferences.showDates;
	}

	public static void setShowDates(boolean showDates) 
	{
		savePreference(SHOW_DATES, Preferences.showDates = showDates);
	}

	public static boolean isDetectLinks() 
	{
		return Preferences.detectLinks;
	}

	public static void setDetectLinks(boolean detectLinks) 
	{
		savePreference(DETECT_LINKS, Preferences.detectLinks = detectLinks);
	}

	public static int getSortOrder() 
	{
		return Preferences.sortOrder;
	}

	public static void setSortOrder(int sortOrder)
	{
		Preferences.sortOrder = sortOrder;
		Preferences.savePreference(SORT_ORDER, sortOrder);
	}

	public static void InitInstance(Activity a)
	{
		singletonInstance = new Preferences();
		settings = a.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		editor = settings.edit();
		email = getPreferenceString(EMAIL);
		password = getPreferenceString(PASSWORD);
		numLines = getPreferenceInt(NUM_ROWS);
		sortOrder = getPreferenceInt(SORT_ORDER);
		lockOrientation = getPreferenceBoolean(LOCK_ORIENTATION);
		webSync = getPreferenceBoolean(WEB_SYNC);
		showDates = getPreferenceBoolean(SHOW_DATES);
		detectLinks = getPreferenceBoolean(DETECT_LINKS);
	}
	
	public static Preferences getInstance()
	{
		return singletonInstance;
	}
	
	public static void savePreference(String name, String value)
	{
		if(singletonInstance != null)
		{
	    	editor.putString(name, value);
	        // Don't forget to commit your edits!!!
	    	editor.commit();	 
		}
	}
	
	public static void savePreference(String name, int value)
	{
		if(singletonInstance != null)
		{
			editor.putInt(name, value);
	        // Don't forget to commit your edits!!!
	    	editor.commit();	 
		}
	}
	
	public static void savePreference(String name, boolean value)
	{
		if(singletonInstance != null)
		{
			editor.putBoolean(name, value);
	        // Don't forget to commit your edits!!!
	    	editor.commit();	 
		}
	}
	
	public static String getPreferenceString(String name)
	{
		return settings.getString(name, null);
	}
	
	public static int getPreferenceInt(String name)
	{
		return settings.getInt(name, 1);
	}
	
	public static boolean getPreferenceBoolean(String name)
	{
		return settings.getBoolean(name, false);
	}
	
	public static String getPassword() 
	{
		return Preferences.password;
	}
	
	public static void setPassword(String password) 
	{
		savePreference(PASSWORD, Preferences.password = password);
	}
	
	public static void setEmail(String email) 
	{
		savePreference(EMAIL, Preferences.email = email);
	}
	
	public static String getEmail()
	{
		return Preferences.email;
	}

	public static int getNumLines() 
	{
		return Preferences.numLines;
	}

	public static void setNumLines(int numLines)
	{
		savePreference(NUM_ROWS, Preferences.numLines = numLines);
	}
}
