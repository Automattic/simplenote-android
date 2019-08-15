package com.automattic.simplenote.utils;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SearchTokenizerTest {

    @Test
    public void testPerformsLiteralQuery() {
        SearchTokenizer tokenizer = new SearchTokenizer("/hello world");

        assertThat(tokenizer.toString(), is("hello world"));
    }

    @Test
    public void testFixesMalformedLiteralQuery() {
        SearchTokenizer tokenizer = new SearchTokenizer("/\"hello\" \"world");

        assertThat(tokenizer.toString(), is("\"hello\" \"world\""));
    }

    @Test
    public void testDetectsTokensAndAddsGlobs() {
        SearchTokenizer tokenizer = new SearchTokenizer("hello world");

        assertThat(tokenizer.toString(), is("hello* world*"));
    }

    @Test
    public void testDetectsStrictSearchStrings() {
        SearchTokenizer tokenizer = new SearchTokenizer("\"hello world\" lorem");

        assertThat(tokenizer.toString(), is("\"hello world\" lorem*"));
    }

    @Test
    public void testDetectsFieldScopes() {
        SearchTokenizer tokenizer = new SearchTokenizer("tag: something");

        assertThat(tokenizer.toString(), is("tag: something*"));
    }

    @Test
    public void testDetectsEscapedQuotes() {
        SearchTokenizer tokenizer = new SearchTokenizer("this is \\\" an escaped quote");
        assertThat(tokenizer.toString(), is("this* is* \\\"* an* escaped* quote*"));

        tokenizer = new SearchTokenizer("this is \\' an escaped quote");
        assertThat(tokenizer.toString(), is("this* is* \\'* an* escaped* quote*"));
    }

    @Test
    public void testMixedQuotes() {
        SearchTokenizer tokenizer = new SearchTokenizer("quote in 'a \" quote");
        assertThat(tokenizer.toString(), is("quote* in* 'a \" quote'"));

        tokenizer = new SearchTokenizer("quote in \"a ' quote\"");
        assertThat(tokenizer.toString(), is("quote* in* \"a ' quote\""));
    }
}