package com.automattic.simplenote.models;

import junit.framework.TestCase;

import com.simperium.client.Bucket;

import com.simperium.test.MockBucket;

public class NoteTaggerTest extends TestCase {

    public void testURLEncodeTagKey()
    throws Exception {
        Bucket<Note> notesBucket = MockBucket.buildBucket(new Note.Schema());
        Bucket<Tag> tagsBucket = MockBucket.buildBucket(new Tag.Schema());

        Note note = notesBucket.newObject("hola-mundo");
        note.setTagString("normal spécial");

        NoteTagger tagger = new NoteTagger(tagsBucket);
        tagger.onSaveObject(notesBucket, note);

        // there should be two tags now
        assertEquals(2, tagsBucket.count());

        // spécial should be url encoded to sp%C3%A9cial
        Tag special = tagsBucket.getObject("sp%C3%A9cial");
        assertNotNull(special);
        assertEquals("spécial", special.getName());

    }

}