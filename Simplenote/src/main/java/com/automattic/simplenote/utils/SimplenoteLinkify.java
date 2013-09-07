package com.automattic.simplenote.utils;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.widget.TextView;

/**
 * Created by dan on 9/6/13.
 */
public class SimplenoteLinkify {

    // Works the same as Linkify.addLinks, but doesn't set movement method
    public static final boolean addLinks(TextView text, int mask) {
        if (mask == 0) {
            return false;
        }

        CharSequence t = text.getText();

        if (t instanceof Spannable) {
            if (Linkify.addLinks((Spannable) t, mask)) {
                return true;
            }

            return false;
        } else {
            SpannableString s = SpannableString.valueOf(t);

            if (Linkify.addLinks(s, mask)) {
                text.setText(s);
                return true;
            }

            return false;
        }
    }
}
