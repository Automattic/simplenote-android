package com.automattic.simplenote.utils;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChecklistsTest {
    @Test
    public void testRegexMatching() {
        String checklistMarkdown = "ToDo\n- [ ] Write code\n- [ ] Test it\n- [ ]Ship it - [x] not on a newline";

        Pattern p = Pattern.compile(ChecklistUtils.ChecklistRegexLineStart, Pattern.MULTILINE);
        Matcher m = p.matcher(checklistMarkdown);

        int count = 0;
        while(m.find()) {
            count++;
        }

        assertThat(count, is(3));

        p = Pattern.compile(ChecklistUtils.ChecklistRegex, Pattern.MULTILINE);
        m = p.matcher(checklistMarkdown);

        count = 0;
        while(m.find()) {
            count++;
        }

        assertThat(count, is(4));
    }
}
