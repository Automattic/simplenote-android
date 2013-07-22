package com.automattic.simplenote.models;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;

public class NoteTest extends TestCase {

    protected Note mNote;

    protected void setUp() throws Exception {
        super.setUp();
        mNote = new Note("test");
    }

    public void testKey(){
        assertEquals("test", mNote.getSimperiumKey());
    }

    public void testTagString(){
        List<String> tags = tagList("one", "two", "three", "four");

        mNote.setTagString(" one two three  four ");

        assertEquals(tags, mNote.getTags());
        assertEquals("one two three four", mNote.getTagString().toString());
    }

    public void testRemoveDupsFromTagString(){
        List<String> tags = tagList("one", "two", "three", "four");

        mNote.setTagString(" one two two three two four ");

        assertEquals(tags, mNote.getTags());
        assertEquals("one two three four", mNote.getTagString().toString());
    }

    public void testParseTitleAndPreview(){
        String title = "Lorem ipsum dolor sit amet,";
        String preview = "consectetur adipisicing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.";

        mNote.setContent("Lorem ipsum dolor sit amet,\n"
            + "consectetur adipisicing elit,\n"
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n"
            + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.");

        assertEquals(title, mNote.getTitle());
        assertEquals(preview, mNote.getContentPreview());
    }

    protected List<String> tagList(String ... tags){
        List<String> tagArray = new ArrayList<String>(tags.length);
        for (String tag : tags) {
            tagArray.add(tag);
        }
        return tagArray;
    }

}