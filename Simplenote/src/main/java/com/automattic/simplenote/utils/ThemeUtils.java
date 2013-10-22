package com.automattic.simplenote.utils;

import com.automattic.simplenote.R;

import android.view.ContextThemeWrapper;

public class ThemeUtils {

    // theme constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static void setTheme(ContextThemeWrapper context){
        int theme = PrefUtils.getIntPref(context, PrefUtils.PREF_THEME, THEME_LIGHT);
        if (theme == THEME_LIGHT)
            context.setTheme(R.style.Theme_Simplestyle);
        else
            context.setTheme(R.style.Theme_Simplestyle_Dark);
    }

}