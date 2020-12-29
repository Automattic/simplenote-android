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

    @Test
    public void testConvertNumberToDateString() {
        String dateString = "2020-12-29T14:52:00.000Z";
        String dateConverted = Note.numberToDateString(1609253520);

        assertThat(dateConverted, is(dateString));
    }

    @Test
    public void testGetContent() {
        String content = "Lorem ipsum dolor sit amet,\n"
                + "consectetur adipisicing elit,\n"
                + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n"
                + "Duis aute irure dolor in reprehenderit in voluptate velit esse cil.";

        mNote.setContent("Lorem ipsum dolor sit amet,\n"
                + "consectetur adipisicing elit,\n"
                + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n"
                + "Duis aute irure dolor in reprehenderit in voluptate velit esse cil.");

        assertThat(mNote.getContent(), is(content));
    }

    @Test
    public void testEnabledAndDisabledPreview() {
        Note note = new Note("Test");

        note.setPreviewEnabled(true);
        assertThat(note.isPreviewEnabled(), is(true));

        note.setPreviewEnabled(false);
        assertThat(note.isPreviewEnabled(), is(false));
    }

    @Test
    public void testEnabledAndDisabledMarkdown() {
        Note note = new Note("Test");

        note.setMarkdownEnabled(true);
        assertThat(note.isMarkdownEnabled(), is(true));

        note.setMarkdownEnabled(false);
        assertThat(note.isMarkdownEnabled(), is(false));
    }

    @Test
    public void testDeletedAndUndeletedNote() {
        Note note = new Note("note-test");

        note.setDeleted(true);
        assertThat(note.isDeleted(), is(true));

        note.setDeleted(false);
        assertThat(note.isDeleted(), is(false));
    }

    @Test
    public void testNoteHasChanges() {

        String content = "Lorem ipsum dolor sit amet";
        String tag = "tag";
        boolean isPinned = true;
        boolean isMarkdownEnabled = true;
        boolean isPreviewEnabled = true;

        Note note = new Note("note-test");
        note.setContent(content);
        note.setTagString(tag);
        note.setPinned(isPinned);
        note.setMarkdownEnabled(isMarkdownEnabled);
        note.setPreviewEnabled(isPreviewEnabled);

        assertThat(note.hasChanges(
                content, tag, isPinned, isMarkdownEnabled, isPreviewEnabled
        ), is(false));

        note.setContent("new content");
        note.setTagString("new tag");
        note.setPinned(false);
        note.setMarkdownEnabled(false);
        note.setPreviewEnabled(false);

        assertThat(note.hasChanges(
                content, tag, isPinned, isMarkdownEnabled, isPreviewEnabled
        ), is(true));

    }

}