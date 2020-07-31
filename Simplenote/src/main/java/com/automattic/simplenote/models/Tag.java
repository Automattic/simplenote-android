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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    public void renameTo(String tagOld, String tagNew, int index, Bucket<Note> notesBucket) throws BucketObjectNameInvalid {
        String tagHash = getSimperiumKey();

        if (!tagHash.equals(TagUtils.hashTag(tagNew))) {
            // create a new tag with the value as the key/name
            //noinspection unchecked
            TagUtils.createTag((Bucket<Tag>) getBucket(), tagNew, index);
            // get all the notes from tag, remove the item
            ObjectCursor<Note> notesCursor = findNotes(notesBucket, tagOld);

            while (notesCursor.moveToNext()) {
                Note note = notesCursor.getObject();
                List<String> tags = new ArrayList<>(note.getTags());
                List<String> newTags = new ArrayList<>(tags.size());
                List<String> tagHashes = new ArrayList<>();
                Set<String> tagSet = new HashSet<>();

                // Create list and set of hashed tag to compare.
                for (String tag : tags) {
                    String hash = TagUtils.hashTag(tag);
                    tagHashes.add(hash);
                    tagSet.add(hash);
                }

                // Go through all note's tags and compare hashed tags.
                for (int i = 0; i < tags.size(); i++) {
                    // Add new tag if it's this tag and note doesn't already have same tag.
                    if (tagHash.equals(tagHashes.get(i)) && !tagSet.contains(tagHashes.get(i))) {
                        newTags.add(tagNew);
                    // Add tag if it's not old tag to be deleted.
                    } else if (!tags.get(i).equals(tagOld)) {
                        newTags.add(tags.get(i));
                    }
                }

                note.setTags(newTags);
                note.save();
            }

            notesCursor.close();
            delete();
        } else if (!getName().equals(tagNew)) {
            setName(tagNew);
            save();
        }
    }

    public ObjectCursor<Note> findNotes(Bucket<Note> notesBucket, String name) {
        return notesBucket.query().where(TAGS_PROPERTY, ComparisonType.LIKE, name).execute();
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