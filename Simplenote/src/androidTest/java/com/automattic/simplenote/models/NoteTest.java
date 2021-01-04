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

    private static final String CONTENT_PREVIEW = "consectetur adipisicing elit,"
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
            + "Duis aute irure dolor in reprehenderit in voluptate velit esse cil.";
    private static final String CONTENT_TITLE = "Lorem ipsum dolor sit amet";
    private static final String CONTENT = CONTENT_TITLE + "\n" + CONTENT_PREVIEW;
    private static final String KEY = "test";
    private Note mNote;

    @Before
    public void setUp() {
        mNote = new Note(KEY);
    }

    @Test
    public void testKey() {
        assertThat(mNote.getSimperiumKey(), is(KEY));
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
        mNote.setContent(CONTENT);
        assertThat(mNote.getTitle(), is(CONTENT_TITLE));
        assertThat(mNote.getContentPreview(), is(CONTENT_PREVIEW));
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
        mNote.setPinned(true);
        assertThat(mNote.isPinned(), is(true));
        mNote.setPinned(false);
        assertThat(mNote.isPinned(), is(false));
    }

    private List<String> tagList(String... tags) {
        List<String> tagArray = new ArrayList<>(tags.length);
        Collections.addAll(tagArray, tags);
        return tagArray;
    }

    @Test
    public void testGetContent() {
        mNote.setContent(CONTENT);
        assertThat(mNote.getContent(), is(CONTENT));
    }

    @Test
    public void testEnabledAndDisabledPreview() {
        mNote.setPreviewEnabled(true);
        assertThat(mNote.isPreviewEnabled(), is(true));
        mNote.setPreviewEnabled(false);
        assertThat(mNote.isPreviewEnabled(), is(false));
    }

    @Test
    public void testEnabledAndDisabledMarkdown() {
        mNote.setMarkdownEnabled(true);
        assertThat(mNote.isMarkdownEnabled(), is(true));
        mNote.setMarkdownEnabled(false);
        assertThat(mNote.isMarkdownEnabled(), is(false));
    }

    @Test
    public void testDeletedAndUndeletedNote() {
        mNote.setDeleted(true);
        assertThat(mNote.isDeleted(), is(true));
        mNote.setDeleted(false);
        assertThat(mNote.isDeleted(), is(false));
    }

    @Test
    public void testNoteHasChanges() {
        String tag = "tag";
        boolean isPinned = true;
        boolean isMarkdownEnabled = true;
        boolean isPreviewEnabled = true;

        Note note = new Note("note-test");
        note.setContent(CONTENT);
        note.setTagString(tag);
        note.setPinned(isPinned);
        note.setMarkdownEnabled(isMarkdownEnabled);
        note.setPreviewEnabled(isPreviewEnabled);

        assertThat(note.hasChanges(
                CONTENT, tag, isPinned, isMarkdownEnabled, isPreviewEnabled
        ), is(false));

        note.setContent("New content");
        note.setTagString("New tag");
        note.setPinned(false);
        note.setMarkdownEnabled(false);
        note.setPreviewEnabled(false);

        assertThat(note.hasChanges(
                CONTENT, tag, isPinned, isMarkdownEnabled, isPreviewEnabled
        ), is(true));

    }

}