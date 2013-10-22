package com.automattic.simplenote.models;

import android.content.ContentValues;

import com.simperium.client.FullTextIndex;

public class NoteFullTextIndexer implements FullTextIndex.Indexer<Note> {

    static public final String COMMA = ", ";
    static public final String[] INDEXES = Note.FULL_TEXT_INDEXES;

    @Override
    public ContentValues index(String[] keys, Note note){
        ContentValues values = new ContentValues(keys.length);
        values.put(INDEXES[0], note.getTitle());
        values.put(INDEXES[1], note.getContent());
        return values;
    }

}