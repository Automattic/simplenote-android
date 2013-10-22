package com.automattic.simplenote.models;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;

import com.simperium.client.BucketSchema.Index;

public class NoteIndexerTest extends TestCase {

    protected NoteIndexer mIndexer;

    protected void setUp()
    throws Exception {
        super.setUp();

        mIndexer = new NoteIndexer();
    }

    public void testIndexMillisecondDates()
    throws Exception {
        JSONObject properties = new JSONObject();
        properties.put(Note.MODIFICATION_DATE_PROPERTY, 1379018823037.0);
        properties.put(Note.CREATION_DATE_PROPERTY, 1379018823037.0);

        Note note = new Note("test", properties);
        List<Index> indexes = mIndexer.index(note);

        assertIndex(indexes, Note.MODIFIED_INDEX_NAME, 1379018823000L);
        assertIndex(indexes, Note.CREATED_INDEX_NAME, 1379018823000L);

    }


    public void testConvertMillisToSeconds()
    throws Exception {
        Calendar date = Note.numberToDate(1379018823037.0);
        assertEquals(1379018823000L, date.getTimeInMillis());
    }

    public static void assertIndex(List<Index> indexes, String name, Object value) {
        for (Index index : indexes) {
            if (index.getName().equals(name)){
                assertEquals(String.format("Index %s had incorrect value", name), value, index.getValue());
                return;
            }
        }
        fail(String.format("Did not find a index with name: %s", name));
    }

}