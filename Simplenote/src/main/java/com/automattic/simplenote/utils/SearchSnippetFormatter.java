package com.automattic.simplenote.utils;

import android.text.SpannableString;

public class SearchSnippetFormatter {

    static public final char OPEN_BRACKET = '<';
    static public final String OPEN_MATCH = "<match>";
    static public final String CLOSE_MATCH = "</match>";

    public static SpannableString formatString(String snippet) {
        return (new SearchSnippetFormatter(snippet)).toSpannableString();
    }

    private String mSnippet;

    public SearchSnippetFormatter(String snippet) {
        mSnippet = snippet;
    }

    private SpannableString parseSnippet(){
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int length = mSnippet.length();
        boolean inMatch = false;

        char current;
        for (int position=0; position < length; position++) {
            current = mSnippet.charAt(position);

            if (current == OPEN_BRACKET) {
                // check if we opened a match
            }

        }
    }

    public SpannableString toSpannableString() {
        return parseSnippet();
    }

}