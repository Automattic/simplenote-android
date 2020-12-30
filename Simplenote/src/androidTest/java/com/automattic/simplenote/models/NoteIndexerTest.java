package com.automattic.simplenote.models;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.simperium.client.BucketSchema.Index;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.List;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NoteIndexerTest {
    private static final String DATE_STRING = "2013-09-12T20:47:03.000Z";
    private static final double DATE_DOUBLE = 1379018823037.0;
    private static final long DATE_LONG = 1379018823000L;
    private static final long DATE_NUMBER = 1379018823;

    private Note mNote;
    private NoteIndexer mIndexer;

    @Before
    public void setUp() throws JSONException {
        JSONObject properties = new JSONObject();
        properties.put(Note.CREATION_DATE_PROPERTY, DATE_DOUBLE);
        properties.put(Note.MODIFICATION_DATE_PROPERTY, DATE_DOUBLE);
        properties.put(Note.PUBLISH_URL_PROPERTY, "test.url.property");
        mNote = new Note("test", properties);
        mIndexer = new NoteIndexer();
    }

    @Test
    public void testIndexMillisecondDates() {
        List<Index> indexes = mIndexer.index(mNote);
        assertIndex(indexes, Note.MODIFIED_INDEX_NAME, DATE_LONG);
        assertIndex(indexes, Note.CREATED_INDEX_NAME, DATE_LONG);
    }


    @Test
    public void testConvertMillisToSeconds() {
        Calendar date = Note.numberToDate(DATE_DOUBLE);
        assertThat(date.getTimeInMillis(), is(DATE_LONG));
    }

    @Test
    public void testConvertNumberToDateString() {
        String dateConverted = Note.numberToDateString(DATE_NUMBER);
        assertThat(dateConverted, is(DATE_STRING));
    }

    @Test
    public void testGetCreationDate() {
        Calendar calendar = mNote.getCreationDate();
        assertThat(calendar.getTimeInMillis(), is(DATE_LONG));
    }

    @Test
    public void testGetCreationDateString() {
        Calendar date = Note.numberToDate(DATE_DOUBLE);
        mNote.setCreationDate(date);
        assertThat(mNote.getCreationDateString(), is(DATE_STRING));
    }

    @Test
    public void testGetModificationDate() {
        Calendar calendar = mNote.getModificationDate();
        assertThat(calendar.getTimeInMillis(), is(DATE_LONG));
    }

    @Test
    public void testGetModificationDateString() {
        Calendar date = Note.numberToDate(DATE_DOUBLE);
        mNote.setModificationDate(date);
        assertThat(mNote.getModificationDateString(), is(DATE_STRING));
    }

    @Test
    public void testIsPublished() {
        mNote.setPublished(true);
        assertThat(mNote.isPublished(), is(true));
    }

    @Test
    public void testIsNotPublished(){
        mNote.setPublished(false);
        assertThat(mNote.isPublished(), is(false));
    }

    private void assertIndex(List<Index> indexes, String name, final Object expected) {
        for (Index index : indexes) {
            if (index.getName().equals(name)) {
                assertThat(String.format("Index %s had incorrect value", name), index.getValue(), is(expected));
                return;
            }
        }
        fail(String.format("Did not find a index with name: %s", name));
    }

}