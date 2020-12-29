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

    private NoteIndexer mIndexer;

    @Before
    public void setUp() {
        mIndexer = new NoteIndexer();
    }

    @Test
    public void testIndexMillisecondDates() throws JSONException {
        JSONObject properties = new JSONObject();
        properties.put(Note.MODIFICATION_DATE_PROPERTY, 1379018823037.0);
        properties.put(Note.CREATION_DATE_PROPERTY, 1379018823037.0);

        Note note = new Note("test", properties);
        List<Index> indexes = mIndexer.index(note);

        assertIndex(indexes, Note.MODIFIED_INDEX_NAME, 1379018823000L);
        assertIndex(indexes, Note.CREATED_INDEX_NAME, 1379018823000L);
    }


    @Test
    public void testConvertMillisToSeconds() {
        Calendar date = Note.numberToDate(1379018823037.0);

        assertThat(date.getTimeInMillis(), is(1379018823000L));
    }

    @Test
    public void testGetCreationDate() throws JSONException {
        JSONObject properties = new JSONObject();
        properties.put(Note.CREATION_DATE_PROPERTY, 1379018823037.0);

        Note note = new Note("test", properties);
        Calendar calendar = note.getCreationDate();

        assertThat(calendar.getTimeInMillis(), is(1379018823000L));
    }

    @Test
    public void testGetCreationDateString() {
        Calendar date = Note.numberToDate(1379018823037.0);
        Note note = new Note("test");
        note.setCreationDate(date);

        assertThat(note.getCreationDateString(), is("2013-09-12T20:47:03.000Z"));
    }

    @Test
    public void testGetModificationDate() throws JSONException {
        JSONObject properties = new JSONObject();
        properties.put(Note.MODIFICATION_DATE_PROPERTY, 1379018823037.0);

        Note note = new Note("test", properties);
        Calendar calendar = note.getModificationDate();

        assertThat(calendar.getTimeInMillis(), is(1379018823000L));
    }

    @Test
    public void testGetModificationDateString() {
        Calendar date = Note.numberToDate(1379018823037.0);
        Note note = new Note("test");
        note.setModificationDate(date);

        assertThat(note.getModificationDateString(), is("2013-09-12T20:47:03.000Z"));
    }

    @Test
    public void testIsPublished() throws JSONException {
        JSONObject properties = new JSONObject();
        properties.put(Note.PUBLISH_URL_PROPERTY, "test.url.property");

        Note note = new Note("test", properties);

        note.setPublished(true);
        assertThat(note.isPublished(), is(true));

        note.setPublished(false);
        assertThat(note.isPublished(), is(false));
    }

    @Test
    public void testIsNotPublished(){
        Note note = new Note("test");

        note.setPublished(true);
        assertThat(note.isPublished(), is(false));
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