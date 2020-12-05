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

    public void renameTo(String tagOld, String tagNew, int index, Bucket<Note> notesBucket) throws BucketObjectNameInvalid {
        // When old tag ID is equal to new tag hash, tag is being renamed to lexical variation.
        boolean isOldIdEqualToNewHash = getSimperiumKey().equals(TagUtils.hashTag(tagNew));
        //noinspection unchecked
        Bucket<Tag> tagsBucket = (Bucket<Tag>) getBucket();
        // Get all notes with old tag to update.
        ObjectCursor<Note> notes = findNotes(notesBucket, tagOld);

        while (notes.moveToNext()) {
            Note note = notes.getObject();
            List<String> tagsNew = new ArrayList<>();
            List<String> tagsHash = new ArrayList<>();

            // Create lists of note's tags excluding old tag.
            for (String tag : note.getTags()) {
                if (!tag.equals(tagOld)) {
                    tagsNew.add(tag);
                    tagsHash.add(TagUtils.hashTag(tag));
                }
            }

            // Add lexical tag to note.  Update this tag's name and save it.
            if (isOldIdEqualToNewHash) {
                tagsNew.add(tagNew);

                if (!getName().equals(tagNew)) {
                    setName(tagNew);
                    save();
                }
            // Add new canonical tag to note and create new tag.  Delete this tag.
            } else {
                // Add new tag if note doesn't already have same hashed tag.
                if (!tagsHash.contains(TagUtils.hashTag(tagNew))) {
                    tagsNew.add(TagUtils.getCanonicalFromLexical(tagsBucket, tagNew));
                }

                // Create new tag if canonical tag doesn't already exist.
                if (!TagUtils.hasCanonicalOfLexical(tagsBucket, tagNew)) {
                    TagUtils.createTag(tagsBucket, tagNew, index);
                }

                delete();
            }

            // Add new tags to note and save it.
            note.setTags(tagsNew);
            note.save();
        }

        notes.close();
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