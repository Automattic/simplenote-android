package com.automattic.simplenote.utils;

import junit.framework.TestCase;

public class SearchTokenizerTest extends TestCase {

    public void testPerformsLiteralQuery()
    throws Exception {
        SearchTokenizer tokenizer = new SearchTokenizer("/hello world");
        assertEquals("hello world", tokenizer.toString());
    }

    public void testFixesMalformedLiteralQuery(){
        SearchTokenizer tokenizer = new SearchTokenizer("/\"hello\" \"world");
        assertEquals("\"hello\" \"world\"", tokenizer.toString());
    }

    public void testDetectsTokensAndAddsGlobs()
    throws Exception {
        SearchTokenizer tokenizer = new SearchTokenizer("hello world");
        assertEquals("hello* world*", tokenizer.toString());
    }

    public void testDetectsStrictSearchStrings()
    throws Exception {
        SearchTokenizer tokenizer = new SearchTokenizer("\"hello world\" lorem");
        assertEquals("\"hello world\" lorem*", tokenizer.toString());
    }

    public void testDetectsFieldScopes()
    throws Exception {
        SearchTokenizer tokenizer = new SearchTokenizer("tag: something");
        assertEquals("tag: something*", tokenizer.toString());
    }

    public void testDetectsEscapedQuotes()
    throws Exception {
        SearchTokenizer tokenizer = new SearchTokenizer("this is \\\" an escaped quote");
        assertEquals("this* is* \\\"* an* escaped* quote*", tokenizer.toString());

        tokenizer = new SearchTokenizer("this is \\' an escaped quote");
        assertEquals("this* is* \\'* an* escaped* quote*", tokenizer.toString());

    }

    public void testMixedQuotes()
    throws Exception {

        SearchTokenizer tokenizer = new SearchTokenizer("quote in 'a \" quote");
        assertEquals("quote* in* 'a \" quote'", tokenizer.toString());

        tokenizer = new SearchTokenizer("quote in \"a ' quote\"");
        assertEquals("quote* in* \"a ' quote\"", tokenizer.toString());

    }

}