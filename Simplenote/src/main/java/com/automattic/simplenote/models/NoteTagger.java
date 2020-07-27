package com.automattic.simplenote.models;

import android.util.Log;

import com.automattic.simplenote.utils.TagUtils;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;

import java.util.List;

/**
 * Listens to the notes bucket and creates tags for any non-existent tags in the tags bucket.
 */
public class NoteTagger implements Bucket.Listener<Note> {
    private Bucket<Tag> mTagsBucket;

    public NoteTagger(Bucket<Tag> tagsBucket) {
        mTagsBucket = tagsBucket;
    }

    /*
    * When a note is saved check its array of tags to make sure there is a corresponding tag
    * object and create one if necessary. Re-save all tags so their indexes are updated.
    * */
    @Override
    public void onSaveObject(Bucket<Note> bucket, Note note) {
        // make sure we have tags
        List<String> tags = note.getTags();

        for (String name : tags) {
            try {
                TagUtils.createTagIfMissing(mTagsBucket, name);
            } catch (BucketObjectNameInvalid e) {
                Log.e("Simplenote.NoteTagger", "Invalid tag name " + "\"" + name + "\"", e);
            }
        }
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
    }

    @Override
    public void onNetworkChange(Bucket<Note> note, Bucket.ChangeType changeType, String key) {
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> bucket, Note object) {
    }
}