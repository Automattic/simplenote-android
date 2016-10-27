package com.automattic.simplenote.models;

import com.simperium.client.FullTextIndex;

import java.util.HashMap;
import java.util.Map;

public class NoteFullTextIndexer implements FullTextIndex.Indexer<Note> {

    @SuppressWarnings("unused")
    static public final String COMMA = ", ";
    static public final String[] INDEXES = Note.FULL_TEXT_INDEXES;

    @Override
    public Map<String, String> index(String[] keys, Note note) {
        Map<String, String> values = new HashMap<>(keys.length);
        values.put(INDEXES[0], note.getTitle());
        values.put(INDEXES[1], note.getContent());
        return values;
    }

}