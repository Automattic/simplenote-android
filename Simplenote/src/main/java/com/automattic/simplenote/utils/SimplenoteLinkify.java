package com.automattic.simplenote.utils;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.widget.TextView;

import java.util.regex.Pattern;

public class SimplenoteLinkify {
    public static final String SIMPLENOTE_SCHEME = "simplenote://";
    public static final String SIMPLENOTE_LINK_PREFIX = SIMPLENOTE_SCHEME + "note/";
    public static final String SIMPLENOTE_LINK_ID = "([a-zA-Z0-9_\\.\\-%@]{1,256})";
    public static final Pattern SIMPLENOTE_LINK_PATTERN = Pattern.compile(SIMPLENOTE_LINK_PREFIX + SIMPLENOTE_LINK_ID);

    // Works the same as Linkify.addLinks, but doesn't set movement method
    public static boolean addLinks(TextView text, int mask) {
        if (mask == 0) {
            return false;
        }

        CharSequence t = text.getText();

        if (t instanceof Spannable) {
            boolean linked = Linkify.addLinks((Spannable) t, mask);
            Linkify.addLinks((Spannable) t, SIMPLENOTE_LINK_PATTERN, SIMPLENOTE_SCHEME);

            return linked;
        } else {
            SpannableString s = SpannableString.valueOf(t);

            if (Linkify.addLinks(s, mask)) {
                text.setText(s);
                return true;
            }

            return false;
        }
    }

    public static String getNoteLink(String id) {
        return "(" + SIMPLENOTE_LINK_PREFIX + id + ")";
    }

    public static String getNoteLinkWithTitle(String title, String id) {
        return "[" + title + "]" + getNoteLink(id);
    }
}
