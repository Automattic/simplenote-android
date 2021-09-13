package com.automattic.simplenote.models;

import static org.junit.Assert.assertEquals;

import com.simperium.client.Bucket;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteTest {

    private static final String CONTENT_PREVIEW = "consectetur adipisicing elit,"
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
            + "Duis aute irure dolor in reprehenderit in voluptate velit esse cil.";
    private static final String CONTENT_TITLE = "Lorem ipsum dolor sit amet";
    private static final String CONTENT = CONTENT_TITLE + "\n" + CONTENT_PREVIEW;
    private static final String KEY = "test";
    private Note mNote;
    private Bucket<Note> mockBucket = Mockito.mock(Bucket.class);

    @Before
    public void setUp() {
        mNote = new Note(KEY);
    }

    @Test
    public void testKey() {
        assertEquals(mNote.getSimperiumKey(), KEY);
    }

    @Test
    public void testTagString() {
        mNote.setTagString(" one two three  four ");
        assertEquals(mNote.getTags(), tagList("one", "two", "three", "four"));
        assertEquals(mNote.getTagString().toString(), "one two three four");
    }

    @Test
    public void testTagStringNull() {
        mNote.setTags(tagList("one", "two", "three"));
        mNote.setTagString(null);
        assertEquals(mNote.getTags(), Collections.<String>emptyList());
    }

    @Test
    public void testRemoveDupsFromTagString() {
        mNote.setTagString(" one two tWo three two four ");
        assertEquals(mNote.getTags(), tagList("one", "two", "three", "four"));
        assertEquals(mNote.getTagString().toString(), "one two three four");
    }

    @Test
    public void testParseTitleAndPreview() {
        mNote.setContent(CONTENT);
        assertEquals(mNote.getTitle(), CONTENT_TITLE);
        assertEquals(mNote.getContentPreview(), CONTENT_PREVIEW);
    }

    @Test
    public void testNoteDoesHaveTag() {
        Tag tag = new Tag("tag");
        tag.setName("Tag");
        mNote.setTagString("tag tag2 tag3");
        assertEquals(mNote.hasTag(tag), true);
    }

    @Test
    public void testNoteDoesNotHaveTag() {
        Tag tag = new Tag("tag");
        tag.setName("Tag");
        mNote.setTagString("tag2 tag3");
        assertEquals(mNote.hasTag(tag), false);
    }

    @Test
    public void testPinAndUnpinNote() {
        mNote.setPinned(true);
        assertEquals(mNote.isPinned(), true);
        mNote.setPinned(false);
        assertEquals(mNote.isPinned(), false);
    }

    private List<String> tagList(String... tags) {
        List<String> tagArray = new ArrayList<>(tags.length);
        Collections.addAll(tagArray, tags);
        return tagArray;
    }

    @Test
    public void testGetContent() {
        mNote.setContent(CONTENT);
        assertEquals(mNote.getContent(), CONTENT);
    }

    @Test
    public void testEnabledAndDisabledPreview() {
        mNote.setPreviewEnabled(true);
        assertEquals(mNote.isPreviewEnabled(), true);
        mNote.setPreviewEnabled(false);
        assertEquals(mNote.isPreviewEnabled(), false);
    }

    @Test
    public void testEnabledAndDisabledMarkdown() {
        mNote.setMarkdownEnabled(true);
        assertEquals(mNote.isMarkdownEnabled(), true);
        mNote.setMarkdownEnabled(false);
        assertEquals(mNote.isMarkdownEnabled(), false);
    }

    @Test
    public void testDeletedAndUndeletedNote() {
        mNote.setDeleted(true);
        assertEquals(mNote.isDeleted(), true);
        mNote.setDeleted(false);
        assertEquals(mNote.isDeleted(), false);
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

        assertEquals(note.hasChanges(CONTENT, isPinned, isMarkdownEnabled, isPreviewEnabled), false);

        note.setContent("New content");
        note.setTagString("New tag");
        note.setPinned(false);
        note.setMarkdownEnabled(false);
        note.setPreviewEnabled(false);

        assertEquals(note.hasChanges(CONTENT, isPinned, isMarkdownEnabled, isPreviewEnabled), true);

    }

    @Test
    public void testAddTag() {
        List<String> initialTags = new ArrayList<>();
        initialTags.add("tag1");
        initialTags.add("tag2");

        Note note = new Note("note-test");
        note.setContent(CONTENT);
        note.setTags(initialTags);
        note.setBucket(mockBucket);

        note.addTag("tag3");

        List<String> expectedTags = new ArrayList<>();
        expectedTags.add("tag1");
        expectedTags.add("tag2");
        expectedTags.add("tag3");


        assertEquals(note.getTags(), expectedTags);
    }

    @Test
    public void testAddSameCanonicalTagDoesNotDuplicateTag() {
        List<String> initialTags = new ArrayList<>();
        initialTags.add("tag1");
        initialTags.add("tag2");

        Note note = new Note("note-test");
        note.setContent(CONTENT);
        note.setTags(initialTags);
        note.setBucket(mockBucket);

        note.addTag("TAG2");

        assertEquals(note.getTags(), initialTags);
    }

    @Test
    public void testRemoveTag() {
        List<String> initialTags = new ArrayList<>();
        initialTags.add("tag1");
        initialTags.add("tag2");

        Note note = new Note("note-test");
        note.setContent(CONTENT);
        note.setTags(initialTags);
        note.setBucket(mockBucket);

        note.removeTag("tag2");

        List<String> expectedTags = new ArrayList<>();
        expectedTags.add("tag1");

        assertEquals(note.getTags(), expectedTags);
    }

    @Test
    public void testRemoveTagSameCanonicalName() {
        List<String> initialTags = new ArrayList<>();
        initialTags.add("tag1");
        initialTags.add("tag2");

        Note note = new Note("note-test");
        note.setContent(CONTENT);
        note.setTags(initialTags);
        note.setBucket(mockBucket);

        note.removeTag("TAG2");

        List<String> expectedTags = new ArrayList<>();
        expectedTags.add("tag1");

        assertEquals(note.getTags(), expectedTags);
    }
}