package com.automattic.simplenote.utils;

import android.text.Editable;
import android.text.SpannableStringBuilder;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.automattic.simplenote.widgets.CheckableSpan;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChecklistsTest {
    @Test
    public void testRegexMatching() {
        String checklistMarkdown = "ToDo\n- [ ] Write code\n- [ ] Test it\n- [ ] Ship it - [x] not on a newline";

        Pattern p = Pattern.compile(ChecklistUtils.CHECKLIST_REGEX_LINES, Pattern.MULTILINE);
        Matcher m = p.matcher(checklistMarkdown);

        int count = 0;
        while(m.find()) {
            count++;
        }

        assertThat(count, is(3));

        p = Pattern.compile(ChecklistUtils.CHECKLIST_REGEX, Pattern.MULTILINE);
        m = p.matcher(checklistMarkdown);

        count = 0;
        while(m.find()) {
            count++;
        }

        assertThat(count, is(4));
    }

    @Test
    public void testRegexMatchingNested() {
        String checklistMarkdown = "ToDo\n\n- [ ] New Feature\n    - [ ] Write code\n    - [ ] Test it\n    - [ ] Ship it";

        Pattern p = Pattern.compile(ChecklistUtils.CHECKLIST_REGEX_LINES, Pattern.MULTILINE);
        Matcher m = p.matcher(checklistMarkdown);

        int count = 0;
        while(m.find()) {
            count++;
        }

        assertThat(count, is(4));
    }

    @Test
    public void testCheckableSpanParsing() {
        String checklistMarkdown = "ToDo\n- [ ] Write code\n- [ ] Test it\n- [ ] Ship it - [x] not on a newline";

        SpannableStringBuilder builder = new SpannableStringBuilder(checklistMarkdown);

        Editable editable = ChecklistUtils.addChecklistSpansForRegexAndColor(
                InstrumentationRegistry.getTargetContext(),
                builder,
                ChecklistUtils.CHECKLIST_REGEX_LINES,
                android.R.color.black);

        // We should have 3 CheckableSpans
        CheckableSpan[] spans = editable.getSpans(0, editable.length(), CheckableSpan.class);
        assertThat(spans.length, is(3));
    }

    @Test
    public void testNullBuilderPassed() {
        Editable editable = ChecklistUtils.addChecklistSpansForRegexAndColor(
                InstrumentationRegistry.getTargetContext(),
                null,
                ChecklistUtils.CHECKLIST_REGEX_LINES,
                android.R.color.black);

        assertThat(editable.length(), is(0));
    }
}
