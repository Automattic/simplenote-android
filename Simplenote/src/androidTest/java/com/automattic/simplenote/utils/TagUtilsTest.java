package com.automattic.simplenote.utils;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.automattic.simplenote.models.Note;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TagUtilsTest extends TestCase {

    @Test
    public void testRenameTagInNote() {
        Note note = new Note("key");
        note.setTags(tagList("one", "two", "three"));

        TagUtils.renameTagInNote(note, "one", "four");
        assertThat(note.getTags(), is(tagList("four", "two", "three")));

        TagUtils.renameTagInNote(note, "two", "Two");
        assertThat(note.getTags(), is(tagList("four", "Two", "three")));

        TagUtils.renameTagInNote(note, "three", "four");
        assertThat(note.getTags(), is(tagList("four", "Two")));

        TagUtils.renameTagInNote(note, "four", "two");
        assertThat(note.getTags(), is(tagList("Two")));
    }

    private List<String> tagList(String... tags) {
        List<String> tagArray = new ArrayList<>(tags.length);
        Collections.addAll(tagArray, tags);
        return tagArray;
    }

}