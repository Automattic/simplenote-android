/**
 * Listens to the notes bucket and creates tags for any non-existant tags
 * in the tags bucket.
 */
package com.automattic.simplenote.models;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;

import com.simperium.client.Bucket;
import com.simperium.client.Bucket.OnSaveObjectListener;

import java.util.List;

public class NoteTagger implements OnSaveObjectListener<Note> {

    private Bucket<Tag> mTagsBucket;

    public NoteTagger(Bucket<Tag> tagsBucket){
        mTagsBucket = tagsBucket;
    }

    @Override
    public void onSaveObject(Bucket<Note> bucket, Note note){
        // make sure we have tags
        List<String> tags = note.getTags();
        for (String tagName : tags) {
            // find the tag by the lowercase tag string
            String tagKey = tagName.toLowerCase();
            try {
                mTagsBucket.getObject(tagKey);                        
            } catch (Exception e) {
                // tag doesn't exist, so we'll create one using the key
                Tag tag = mTagsBucket.newObject(tagKey);
                tag.setName(tagName);
                tag.setIndex(mTagsBucket.count());
                tag.save();
            }
        }
    }

}