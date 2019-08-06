package com.automattic.simplenote.models;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NoteTest {

    private static final String TEST = "test";
    private Note mNote;

    @Before
    public void setUp() throws Exception {
        mNote = new Note(TEST);
    }

    @Test
    public void testKey() {
        assertThat(mNote.getSimperiumKey(), is(TEST));
    }

    @Test
    public void testTagString() {
        mNote.setTagString(" one two three  four ");

        assertThat(mNote.getTags(), is(tagList("one", "two", "three", "four")));
        assertThat(mNote.getTagString().toString(), is("one two three four"));
    }

    @Test
    public void testTagStringNull() {
        mNote.setTags(tagList("one", "two", "three"));

        mNote.setTagString(null);

        assertThat(mNote.getTags(), is(Collections.<String>emptyList()));
    }

    @Test
    public void testRemoveDupsFromTagString() {
        mNote.setTagString(" one two tWo three two four ");

        assertThat(mNote.getTags(), is(tagList("one", "two", "three", "four")));
        assertThat(mNote.getTagString().toString(), is("one two three four"));
    }

    @Test
    public void testParseTitleAndPreview() {
        String title = "Lorem ipsum dolor sit amet,";
        String preview = "consectetur adipisicing elit, "
                + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
                + "Duis aute irure dolor in reprehenderit in voluptate velit esse cil";

        mNote.setContent("Lorem ipsum dolor sit amet,\n"
                + "consectetur adipisicing elit,\n"
                + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n"
                + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.");

        assertThat(mNote.getTitle(), is(title));
        assertThat(mNote.getContentPreview(), is(preview));
    }

    @Test
    public void testNoteDoesHaveTag() {
        Tag tag = new Tag("tag");
        tag.setName("Tag");

        mNote.setTagString("tag tag2 tag3");

        assertThat(mNote.hasTag(tag), is(true));
    }

    @Test
    public void testNoteDoesNotHaveTag() {
        Tag tag = new Tag("tag");
        tag.setName("Tag");

        mNote.setTagString("tag2 tag3");

        assertThat(mNote.hasTag(tag), is(false));
    }

    @Test
    public void testPinAndUnpinNote() {
        Note note = new Note("note-test");

        note.setPinned(true);
        assertThat(note.isPinned(), is(true));

        note.setPinned(false);
        assertThat(note.isPinned(), is(false));
    }

    private List<String> tagList(String... tags) {
        List<String> tagArray = new ArrayList<>(tags.length);
        Collections.addAll(tagArray, tags);
        return tagArray;
    }

}