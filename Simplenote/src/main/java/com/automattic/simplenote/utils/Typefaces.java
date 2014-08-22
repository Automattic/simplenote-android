package com.automattic.simplenote.utils;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Locale;

public class Typefaces {
    private static final Hashtable<String, Typeface> cache = new Hashtable<String, Typeface>();

    public static Typeface get(Context c, String assetPath) {

        synchronized (cache) {
            if (!cache.containsKey(assetPath)) {
                try {
                    // Check user locale, use default font if in the blacklist
                    String code = Locale.getDefault().getLanguage();
                    cache.put(assetPath, Typeface.createFromAsset(c.getAssets(), assetPath));
                } catch (Exception e) {
                    return null;
                }
            }
            return cache.get(assetPath);
        }
    }
}
