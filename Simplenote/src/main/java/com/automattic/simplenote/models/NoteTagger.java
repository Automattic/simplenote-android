/**
 * Listens to the notes bucket and creates tags for any non-existant tags
 * in the tags bucket.
 */
package com.automattic.simplenote.models;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.util.Uuid;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class NoteTagger implements Bucket.Listener<Note> {

    private static final String KEY_ENCODING = "UTF-8";
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
        for (String tagName : tags) {
            // find the tag by the lowercase tag string
            String tagKey;
            try {
                tagKey = URLEncoder.encode(Uuid.uuid(), KEY_ENCODING); // keys shouldn't be plain text versions of the name, for encryption purposes
                // I couldn't figure out how to do a query successfully, and how long could walking these really take, anyway?
                boolean tagExists = false;
                Bucket.ObjectCursor tagsCursor = mTagsBucket.allObjects();
                while (tagsCursor.moveToNext()) {
                    Tag tag = (Tag)(tagsCursor.getObject());
                    if(tag.getName().equals(tagName)) {
                        tagExists = true;
                        break;
                    }
                }
                if (!tagExists) {
                    try {
                        Tag tag = mTagsBucket.newObject(tagKey);
                        tag.setName(tagName);
                        tag.setIndex(mTagsBucket.count());
                        tag.save();
                    } catch (BucketObjectNameInvalid invalid) {
                        android.util.Log.e("Simplenote.NoteTagger", "Could not create tag object for note", invalid);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                android.util.Log.e("Simplenote.NoteTagger", "Invalid tag key", e);
            }
        }
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onNetworkChange(Bucket<Note> note, Bucket.ChangeType changeType, String key) {
        // noop
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> bucket, Note object) {
        // noop
    }
}
