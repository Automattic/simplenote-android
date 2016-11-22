package com.automattic.simplenote.models;

import android.graphics.Color;
import android.util.Log;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
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

    public void testTagStringNull(){

        mNote.setTags(tagList("one", "two", "three"));

        mNote.setTagString(null);
        assertEquals(new ArrayList<String>(), mNote.getTags());
    }

    public void testRemoveDupsFromTagString(){
        List<String> tags = tagList("one", "two", "three", "four");

        mNote.setTagString(" one two tWo three two four ");

        assertEquals(tags, mNote.getTags());
        assertEquals("one two three four", mNote.getTagString().toString());
    }

//    public void testParseTitleAndPreview(){
//        String title = "Lorem ipsum dolor sit amet,";
//        String preview = "consectetur adipisicing elit,\n"
//            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
//            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n"
//            + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pa";
//
//        mNote.setContent("Lorem ipsum dolor sit amet,\n"
//            + "consectetur adipisicing elit,\n"
//            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
//            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n"
//            + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.");
//
//        assertEquals(title, mNote.getTitle());
//        assertEquals(preview, mNote.getContentPreview());
//    }

    public void testNoteDoesHaveTag(){
        Tag tag = new Tag("tag");
        tag.setName("Tag");

        mNote.setTagString("tag tag2 tag3");

        assertTrue(mNote.hasTag(tag));
    }

    public void testNoteDoesNotHaveTag(){
        Tag tag = new Tag("tag");
        tag.setName("Tag");

        mNote.setTagString("tag2 tag3");

        assertFalse(mNote.hasTag(tag));
    }

    public void testPinAndUnpinNote() {
        Note note = new Note("note-test");
        note.setPinned(true);
        assertTrue(note.isPinned());
        note.setPinned(false);
        assertFalse(note.isPinned());
    }

    public void testSetColor() {
        Note note = new Note("note-test");
        note.setColor(Color.BLUE);
        assertEquals(Color.BLUE, note.getColor());
    }

    public void testResetColor() {
        Note note = new Note("note-test");
        note.setColor(Color.WHITE);
        assertEquals(Color.WHITE, note.getColor());
    }


    public void testReminder() {
        Note note = new Note("note-test");
        note.setReminder(true);
        Calendar calendar = Calendar.getInstance();
        note.setReminderDate(calendar);
        assertTrue(note.hasReminder());
        note.setReminder(false);
        assertFalse(note.hasReminder());

        assertEquals(0, note.numberToDate(calendar.getTimeInMillis()).compareTo(note.getReminderDate()));
    }

    public void testTemplate() {
        Note note = new Note("note-test");
        note.setTemplate(true);
        assertTrue(note.isTemplate());

        note.setTemplate(false);
        assertFalse(note.isTemplate());
    }

    public void testTodo() {
        Note note = new Note("note-test");
        note.setTodo(true);
        assertTrue(note.isTodo());

        ArrayList<String> todos = new ArrayList<String>();
        todos.add("Todo item");
        note.setTodos(todos);
        assertTrue(note.getTodos().equals(todos));

        ArrayList<String> completedTodos = new ArrayList<String>();
        completedTodos.add("Todo item");
        note.setCompletedTodos(completedTodos);
        assertTrue(note.getCompletedTodos().equals(completedTodos));

        note.setTodo(false);
        assertFalse(note.isTodo());
    }

    protected List<String> tagList(String ... tags){
        List<String> tagArray = new ArrayList<>(tags.length);
        Collections.addAll(tagArray, tags);
        return tagArray;
    }

}