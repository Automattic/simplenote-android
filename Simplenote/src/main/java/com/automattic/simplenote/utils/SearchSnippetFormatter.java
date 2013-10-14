package com.automattic.simplenote.utils;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;

public class SearchSnippetFormatter {

    public interface SpanFactory {
        public Object[] buildSpans(String content);
    }

    static public final char OPEN_BRACKET = '<';
    static public final String OPEN_MATCH = "<match>";
    static public final String CLOSE_MATCH = "</match>";

    static public final int OPEN_MATCH_LENGTH = OPEN_MATCH.length();
    static public final int CLOSE_MATCH_LENGTH = CLOSE_MATCH.length();

    public static Spannable formatString(String snippet, SpanFactory factory) {
        return (new SearchSnippetFormatter(factory, snippet)).toSpannableString();
    }


    private String mSnippet;
    private SpanFactory mFactory;

    public SearchSnippetFormatter(SpanFactory factory, String text){
        mSnippet = text;
        mFactory = factory;
    }

    private Spannable parseSnippet(){
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String snippet = mSnippet.replace("\n", " ");
        boolean inMatch = false;

        int position = 0;
        int lastOpen = -1;

        do {
            if (inMatch) {
                int close = snippet.indexOf(CLOSE_MATCH, position);
                if (close == -1){
                    builder.append(snippet.substring(position));
                    break;
                }
                String highlighted = snippet.substring(position, close);
                int start = builder.length();
                builder.append(highlighted);
                int end = builder.length();
                Object[] spans = mFactory.buildSpans(highlighted);
                for (Object span : spans) {
                    builder.setSpan(span, start, end, 0x0);
                }
                inMatch = false;
                position = close + CLOSE_MATCH_LENGTH;
            } else {
                int open = snippet.indexOf(OPEN_MATCH, position);
                if (open == -1) {
                    builder.append(snippet.substring(position));
                    break;
                }
                builder.append(snippet.substring(position, open));
                inMatch = true;
                position = open + OPEN_MATCH_LENGTH;
            }
        } while(position > -1);

        return builder;
    }

    public Spannable toSpannableString() {
        return parseSnippet();
    }

}