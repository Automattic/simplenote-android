package com.automattic.simplenote.utils;

import junit.framework.TestCase;

import android.text.Spannable;

import com.automattic.simplenote.utils.SearchSnippetFormatter.SpanFactory;

public class SearchSnippetFormatterTest extends TestCase {

    public void testFormatSnippet(){
        String snippet = "\u2026 This is just <match>an example</match> of a\n<match>snippet</match> \u2026";
        Spannable formatted = SearchSnippetFormatter.formatString(snippet, new SpanFactory() {
            @Override
            public Object[] buildSpans(String content) {
                return new Object[]{ new Object() };
            }
        });

        assertEquals("\u2026 This is just an example of a snippet \u2026", formatted.toString());

        Object[] spans = formatted.getSpans(0, formatted.length(), Object.class);
        assertEquals(2, spans.length);

        assertEquals(15, formatted.getSpanStart(spans[0]));
        assertEquals(25, formatted.getSpanEnd(spans[0]));

        assertEquals(31, formatted.getSpanStart(spans[1]));
        assertEquals(38, formatted.getSpanEnd(spans[1]));

    }

}