package com.automattic.simplenote.models;

import com.simperium.client.Bucket;
import com.simperium.client.BucketSchema.Index;
import com.simperium.client.BucketSchema.Indexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by beaucollins on 7/26/13.
 */
public class NoteCountIndexer implements Indexer<Tag> {

    private Bucket<Note> mNotesBucket;

    public NoteCountIndexer(Bucket<Note> notesBucket) {
        mNotesBucket = notesBucket;
    }

    @Override
    public List<Index> index(Tag tag){
        List<Index> indexes = new ArrayList<Index>(1);
        int count = Note.allInTag(mNotesBucket, tag.getSimperiumKey()).count();
        indexes.add(new Index(Tag.NOTE_COUNT_INDEX_NAME, count));
        return indexes;
    }

}
