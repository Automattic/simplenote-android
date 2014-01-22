package com.automattic.simplenote.utils;

import junit.framework.TestCase;

import android.text.Spanned;
import android.text.Spannable;
import android.text.SpannableString;

import java.nio.charset.Charset;

public class MatchOffsetHighlighterTest extends TestCase {


    protected MatchOffsetHighlighter.SpanFactory sHighlighter = new MatchOffsetHighlighter.SpanFactory() {

        @Override
        public Object[] buildSpans() {
            return new Object[]{ new Object() };
        }

    };

    // Uses the offsets from a SQLite fulltext offset function call to highlight text
    // matches are a set of 4 integers seperated by a space. The integers are
    // 1. The column from the full text table
    // 2. The index of the term from the search
    // 3. The index of the start of the match
    // 4. The length of the match
    public void testHighlightMatches()
    throws Exception {
        // this represents 3 different matches, but only two of them for column 1 which is what
        // we're asking for
        String matches = "1 0 6 5 0 0 2 1 1 0 18 3";
        Spannable text = new SpannableString("Lorem ipsum dolor sit amet");

        MatchOffsetHighlighter.highlightMatches(text, matches, 1, sHighlighter);
        Object[] spans = text.getSpans(0, text.length(), Object.class);

        assertEquals(2, spans.length);
        assertEquals(6, text.getSpanStart(spans[0]));
        assertEquals(11, text.getSpanEnd(spans[0]));
        assertEquals(18, text.getSpanStart(spans[1]));
        assertEquals(21, text.getSpanEnd(spans[1]));
    }

    public void testHighlithgMultibyteMatches()
    throws Exception {

        char[] seq = new char[] {
            'T', // 84
            'h', // 104
            'e', // 101
            ' ', // 32
            'E', // 69
            'n', // 110
            'd', // 100
            ' ', // 32
            '\u8212', // 8212
            ' ', // 32
            'D', // 68
            'o', // 111
            'o', // 111
            'r', // 114
            's', // 115
            '\n',// 10
            '\n',// 10
            '_', // 95
            'J', // 74
            'i', // 105
            'm', // 109
            ' ', // 32
            'M', // 77
            'o', // 111
            'r', // 114
            'r', // 114
            'i', // 105
            's', // 115
            'o', // 111
            'n', // 110
            '_', // 95
            ' ', // 32
            ' ', // 32
            '\n' // 10
        };

        SpannableString text = new SpannableString(new String(seq, 0, seq.length));
        String matches = "1 0 12 5 2 0 12 5";

        MatchOffsetHighlighter.highlightMatches(text, matches, 2, sHighlighter);
        Object[] spans = text.getSpans(0, text.length(), Object.class);

        assertEquals(1, spans.length);
        assertEquals("Doors", text.toString().substring(text.getSpanStart(spans[0]), text.getSpanEnd(spans[0])));
    }

    public void testOutOfBoundsOffset()
    throws Exception {
        int offset = MatchOffsetHighlighter.getByteOffset("short", 3, 2);
        assertEquals(0, offset);
    }

}