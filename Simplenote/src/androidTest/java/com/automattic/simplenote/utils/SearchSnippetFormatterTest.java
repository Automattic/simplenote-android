package com.automattic.simplenote.utils;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Spannable;

import com.automattic.simplenote.utils.SearchSnippetFormatter.SpanFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayWithSize;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SearchSnippetFormatterTest {

    @Test
    public void testFormatSnippet() {
        String snippet = "\u2026 This is just <match>an example</match> of a\n<match>snippet</match> \u2026";
        Spannable formatted = SearchSnippetFormatter.formatString(snippet, new SpanFactory() {
            @Override
            public Object[] buildSpans(String content) {
                return new Object[]{new Object()};
            }
        });

        assertThat(formatted.toString(), is("\u2026 This is just an example of a snippet \u2026"));

        Object[] spans = formatted.getSpans(0, formatted.length(), Object.class);
        assertThat(spans, arrayWithSize(2));

        assertThat(formatted.getSpanStart(spans[0]), is(15));
        assertThat(formatted.getSpanEnd(spans[0]), is(25));

        assertThat(formatted.getSpanStart(spans[1]), is(31));
        assertThat(formatted.getSpanEnd(spans[1]), is(38));
    }

}