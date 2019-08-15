package com.automattic.simplenote.utils;

import android.text.Spannable;
import android.text.SpannableString;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayWithSize;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MatchOffsetHighlighterTest {

    private MatchOffsetHighlighter.SpanFactory sHighlighter = new MatchOffsetHighlighter.SpanFactory() {
        @Override
        public Object[] buildSpans() {
            return new Object[]{new Object()};
        }
    };

    // Uses the offsets from a SQLite fulltext offset function call to highlight text
    // matches are a set of 4 integers seperated by a space. The integers are
    // 1. The column from the full text table
    // 2. The index of the term from the search
    // 3. The index of the start of the match
    // 4. The length of the match
    @Test
    public void testHighlightMatches() {
        // this represents 3 different matches, but only two of them for column 1 which is what
        // we're asking for
        String matches = "1 0 6 5 0 0 2 1 1 0 18 3";
        Spannable text = new SpannableString("Lorem ipsum dolor sit amet");

        MatchOffsetHighlighter.highlightMatches(text, matches, text.toString(), 1, sHighlighter);
        Object[] spans = text.getSpans(0, text.length(), Object.class);

        assertThat(spans, arrayWithSize(2));
        assertThat(text.getSpanStart(spans[0]), is(6));
        assertThat(text.getSpanEnd(spans[0]), is(11));
        assertThat(text.getSpanStart(spans[1]), is(18));
        assertThat(text.getSpanEnd(spans[1]), is(21));
    }

    @Test
    public void testMatchesWithHindiContent() {
        String matches = "1 0 0 2";
        Spannable sourceText = new SpannableString("मुक्त ज्ञानकोसे");

        MatchOffsetHighlighter.highlightMatches(sourceText, matches, sourceText.toString(), 1, sHighlighter);
        sourceText.getSpans(0, sourceText.length(), Object.class);
    }

    @Test
    public void testHighlightingMultibyteMatches() {
        char[] seq = new char[]{
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

        MatchOffsetHighlighter.highlightMatches(text, matches, text.toString(), 2, sHighlighter);
        Object[] spans = text.getSpans(0, text.length(), Object.class);

        assertThat(spans, arrayWithSize(1));
        assertThat(text.toString().substring(text.getSpanStart(spans[0]), text.getSpanEnd(spans[0])),
                is("Doors"));
    }

    @Test
    public void testOutOfBoundsOffset() {
        // start plus length exceeds bounds
        SpannableString text = new SpannableString("short");
        int offset = MatchOffsetHighlighter.getByteOffset(text, 3, 5);
        assertThat(offset, is(0));

        // start exceeds string bounds
        offset = MatchOffsetHighlighter.getByteOffset(text, 10, 12);
        assertThat(offset, is(0));
    }

}