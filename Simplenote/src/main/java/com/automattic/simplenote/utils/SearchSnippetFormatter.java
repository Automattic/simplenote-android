package com.automattic.simplenote.utils;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

public class SearchSnippetFormatter {

    @SuppressWarnings("unused")
    static public final char OPEN_BRACKET = '<';
    static public final String OPEN_MATCH = "<match>";
    static public final String CLOSE_MATCH = "</match>";
    static public final int OPEN_MATCH_LENGTH = OPEN_MATCH.length();
    static public final int CLOSE_MATCH_LENGTH = CLOSE_MATCH.length();
    private String mSnippet;
    private SpanFactory mFactory;
    private Context mContext;
    private int mChecklistResId;


    public SearchSnippetFormatter(Context context, SpanFactory factory, String text, int checklistResId) {
        mContext = context;
        mSnippet = text;
        mFactory = factory;
        mChecklistResId = checklistResId;
    }

    public static Spannable formatString(Context context, String snippet, SpanFactory factory, int checkListResId) {
        return (new SearchSnippetFormatter(context, factory, snippet, checkListResId)).toSpannableString();
    }

    private Spannable parseSnippet() {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (mSnippet == null)
            return builder;

        String snippet = mSnippet.replace("\n", " ");
        boolean inMatch = false;

        int position = 0;

        do {
            if (inMatch) {
                int close = snippet.indexOf(CLOSE_MATCH, position);
                if (close == -1) {
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
        } while (position > -1);


        // Apply checklist spans if necessary
        if (mContext != null) {
            builder = (SpannableStringBuilder) ChecklistUtils.addChecklistSpansForRegexAndColor(
                    mContext,
                    builder,
                    ChecklistUtils.CHECKLIST_REGEX,
                    mChecklistResId);
        }

        return builder;
    }

    public Spannable toSpannableString() {
        return parseSnippet();
    }

    public interface SpanFactory {
        Object[] buildSpans(String content);
    }

}