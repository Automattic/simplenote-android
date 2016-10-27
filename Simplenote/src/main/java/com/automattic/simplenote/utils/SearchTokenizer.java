package com.automattic.simplenote.utils;

public class SearchTokenizer {

    static public final String EMPTY = "";

    static public final char LITERAL = '/';
    static public final char SPACE = ' ';
    static public final char DOUBLE_QUOTE = '"';
    static public final char SINGLE_QUOTE = '\'';
    static public final char GLOB = '*';
    static public final char COLON = ':';
    static public final char ESCAPE = '\\';

    private String mRawQuery;

    public SearchTokenizer(String query) {
        mRawQuery = query;
    }

    private String parseQuery() {
        if (mRawQuery == null || mRawQuery.equals(EMPTY)) {
            return EMPTY;
        }

        StringBuilder query = new StringBuilder(mRawQuery.length());

        // iterate through each character
        int length = mRawQuery.length();

        // if we have an open " or not
        boolean inStrictTerm = false;

        // if we've detected a search term
        boolean inTerm = false;

        // if we're doing a literal query
        boolean isLiteral = false;

        // if the current char is a single or double quote
        boolean isQuoteChar;

        // if the previous char was an eschape char
        boolean isEscaped = false;

        // the current character
        char current = '\0', last, quoteChar = '\0';

        for (int position = 0; position < length; position++) {

            last = current;
            current = mRawQuery.charAt(position);

            // if the last character was an \ and we weren't already escaped
            isEscaped = last == ESCAPE && !isEscaped;
            // if it's a ' or " and it isn't be escaped
            isQuoteChar = !isEscaped && (current == SINGLE_QUOTE || current == DOUBLE_QUOTE);

            // if query starts with / we're just going to give the complete query
            if (position == 0 && current == LITERAL && length > 1) {
                isLiteral = true;
                continue;
            }

            // we're inside a quoted section and have found another quote, append and loop
            if (inStrictTerm && current == quoteChar) {
                query.append(current);
                inStrictTerm = false;
                inTerm = false;
                continue;
            }

            // we're in a strict term and it's not a ", so append and continue
            if (inStrictTerm) {
                query.append(current);
                continue;
            }

            // we've found a matching end quote
            if (isQuoteChar) {
                quoteChar = current;
                // we were already in a term so end it with a glob
                if (inTerm && !isLiteral) query.append(new char[]{GLOB, SPACE});
                // start the strict term
                query.append(current);
                inStrictTerm = true;
                inTerm = true;
                continue;
            }

            if (current == COLON && inTerm) {
                inTerm = false;
                query.append(current);
                continue;
            }

            if (current == SPACE) {
                if (inTerm && !isLiteral) query.append(GLOB);
                query.append(current);
                inTerm = false;
                continue;
            }

            inTerm = true;
            query.append(current);

        }

        // close the strict term
        if (inStrictTerm) query.append(quoteChar);
        if (inTerm && !inStrictTerm && !isLiteral) query.append(GLOB);

        return query.toString();
    }

    @Override
    public String toString() {
        return parseQuery();
    }


}