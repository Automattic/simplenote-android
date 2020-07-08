package com.automattic.simplenote.models;

import com.automattic.simplenote.utils.TagUtils;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;
import com.simperium.client.Query.ComparisonType;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.automattic.simplenote.models.Note.TAGS_PROPERTY;

public class Tag extends BucketObject {

    private static final String BUCKET_NAME = "tag";
    public static final String NOTE_COUNT_INDEX_NAME = "note_count";
    public static final String NAME_PROPERTY = "name";
    private static final String INDEX_PROPERTY = "index";
    protected String name = "";

    public Tag(String key) {
        super(key, new JSONObject());
    }

    public Tag(String key, JSONObject properties) {
        super(key, properties);
    }

    public static Query<Tag> all(Bucket<Tag> bucket) {
        return bucket.query().order(INDEX_PROPERTY).orderByKey();
    }

    public static Query<Tag> allWithName(Bucket<Tag> bucket) {
        return all(bucket).include(NAME_PROPERTY);
    }

    public static Query<Tag> allSortedAlphabetically(Bucket<Tag> bucket) {
        String lowerCaseOrderBy = String.format(Locale.US, "LOWER(%s)", NAME_PROPERTY);
        return bucket.query().include(NAME_PROPERTY).order(lowerCaseOrderBy);
    }

    public String getName() {
        String name = (String) getProperty(NAME_PROPERTY);
        if (name == null) {
            name = getSimperiumKey();
        }
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            name = "";
        }
        setProperty(NAME_PROPERTY, name);
    }

    public Integer getIndex() {
        return (Integer) getProperty(INDEX_PROPERTY);
    }

    public boolean hasIndex() {
        return getProperty(INDEX_PROPERTY) != null;
    }

    public void setIndex(Integer tagIndex) {
        if (tagIndex == null) {
            getProperties().remove("index");
        } else {
            setProperty("index", tagIndex);
        }
    }

    public void renameTo(String name, int index, Bucket<Note> notesBucket) throws BucketObjectNameInvalid {
        if (!getSimperiumKey().equals(TagUtils.hashTag(name))) {
            // create a new tag with the value as the key/name
            //noinspection unchecked
            TagUtils.createTag((Bucket<Tag>) getBucket(), name, index);
            // get all the notes from tag, remove the item
            ObjectCursor<Note> notesCursor = findNotes(notesBucket);

            while (notesCursor.moveToNext()) {
                Note note = notesCursor.getObject();
                List<String> tags = new ArrayList<>(note.getTags());
                List<String> newTags = new ArrayList<>(tags.size());

                // iterate and do a case insensitive comparison on each tag
                for (String tag : tags) {
                    // if it's this tag, add the new tag
                    if (getSimperiumKey().equals(TagUtils.hashTag(tag))) {
                        newTags.add(name);
                    } else {
                        newTags.add(tag);
                    }
                }

                note.setTags(newTags);
                note.save();
            }

            notesCursor.close();
            delete();
        } else if (!getName().equals(name)) {
            setName(name);
            save();
        }
    }

    public ObjectCursor<Note> findNotes(Bucket<Note> notesBucket) {
        return notesBucket.query().where(TAGS_PROPERTY, ComparisonType.LIKE, getSimperiumKey()).execute();
    }

    public static class Schema extends BucketSchema<Tag> {

        public Schema() {
            autoIndex();
        }

        public String getRemoteName() {
            return Tag.BUCKET_NAME;
        }

        public Tag build(String key, JSONObject properties) {
            return new Tag(key, properties);
        }

        public void update(Tag tag, JSONObject properties) {
            tag.setProperties(properties);
        }

    }
}