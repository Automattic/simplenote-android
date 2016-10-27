package com.automattic.simplenote.models;

import com.simperium.client.BucketSchema.Index;
import com.simperium.client.BucketSchema.Indexer;

import java.util.ArrayList;
import java.util.List;

public class NoteIndexer implements Indexer<Note> {

    @Override
    public List<Index> index(Note note) {

        List<Index> indexes = new ArrayList<>();
        indexes.add(new Index(Note.PINNED_INDEX_NAME, note.isPinned()));
        indexes.add(new Index(Note.CONTENT_PREVIEW_INDEX_NAME, note.getContentPreview()));
        indexes.add(new Index(Note.TITLE_INDEX_NAME, note.getTitle()));
        indexes.add(new Index(Note.MODIFIED_INDEX_NAME, note.getModificationDate().getTimeInMillis()));
        indexes.add(new Index(Note.CREATED_INDEX_NAME, note.getCreationDate().getTimeInMillis()));
        return indexes;

    }

}
