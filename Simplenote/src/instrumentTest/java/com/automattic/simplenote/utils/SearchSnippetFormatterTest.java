package com.automattic.simplenote.utils;

import junit.framework.TestCase;

import android.text.SpannableString;

public class SearchSnippetFormatterTest extends TestCase {

    public void testFormatSnippet(){
        String snippet = "\u2026 This is just <match>an example</match> of a snippet \u2026";
        SpannableString formatted = SearchSnippetFormatter.formatString(snippet);

        assertEquals("\u2026 This is just an example of a snippet \u2026", formatted.toString());

    }

}