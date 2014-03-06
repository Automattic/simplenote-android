package com.automattic.simplenote.utils;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Locale;

public class Typefaces {
    private static final Hashtable<String, Typeface> cache = new Hashtable<String, Typeface>();

    private static String[] languageCodeBlacklist = new String[] {
            "el", //Greek
            "ru", //Russian
            "be", //Belarusian
            "uk", //Ukrainian
            "bg", //Bulgarian
            "sr", //Serbian
            "mk", //Macedonian
            "bs", //Bosnian
            "ku", //Kurdish
            "os", //Ossetian
            "tg", //Tajik
            "mn", //Mongolian
            "ab", //Abkhaz
            "av", //Avarik
            "az", //Azerbaijani
            "ba", //Bashkir
            "cv", //Chuvash
            "kk", //Kazakh
            "ky", //Kyrgyz
            "tt", //Tatar
            "tk", //Turkmen
            "uz", //Uzbek
            "ik", //Inupiaq
            "iu", //Inuktitut
            "kl"  //Kalaallisut, Greenlandic
    };

    public static Typeface get(Context c, String assetPath) {

        synchronized (cache) {
            if (!cache.containsKey(assetPath)) {
                try {
                    // Check user locale, use default font if in the blacklist
                    String code = Locale.getDefault().getLanguage();
                    if (Arrays.asList(languageCodeBlacklist).contains(code)) {
                        cache.put(assetPath, Typeface.create("sans-serif", Typeface.NORMAL));
                    } else {
                        cache.put(assetPath, Typeface.createFromAsset(c.getAssets(), assetPath));
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            return cache.get(assetPath);
        }
    }
}
