package com.automattic.simplenote.utils;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView;

public class LinkTokenizer implements MultiAutoCompleteTextView.Tokenizer {
    private static final Character CHARACTER_BRACKET_CLOSE = ']';
    private static final Character CHARACTER_BRACKET_OPEN = '[';

    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int length = text.length();

        while (i < length) {
            if (text.charAt(i) == CHARACTER_BRACKET_CLOSE) {
                return i;
            } else {
                i++;
            }
        }

        return length;
    }

    @Override
    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;

        while (i > 0 && text.charAt(i - 1) != CHARACTER_BRACKET_OPEN) {
            i--;
        }

        if (i < 1 || text.charAt(i - 1) != CHARACTER_BRACKET_OPEN) {
            return cursor;
        }

        return i;
    }

    @Override
    public CharSequence terminateToken(CharSequence text) {
        if (text instanceof Spanned) {
            SpannableString spannableString = new SpannableString(text + CHARACTER_BRACKET_CLOSE.toString());
            TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, spannableString, 0);
            return spannableString;
        } else {
            return text + CHARACTER_BRACKET_CLOSE.toString();
        }
    }
}
