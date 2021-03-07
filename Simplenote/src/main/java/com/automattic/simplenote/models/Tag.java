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
        if (tagOld.equals(tagNew)) return;

        renameTagInNotes(tagOld, tagNew, notesBucket);

        //noinspection unchecked
        Bucket<Tag> tagsBucket = (Bucket<Tag>) getBucket();

        boolean isOldIdEqualToNewHash = getSimperiumKey().equals(TagUtils.hashTag(tagNew));
        if (isOldIdEqualToNewHash) {
            setName(tagNew);
            save();

        } else {
            // Create new tag if canonical tag doesn't already exist.
            if (!TagUtils.hasCanonicalOfLexical(tagsBucket, tagNew)) {
                TagUtils.createTag(tagsBucket, tagNew, index);
            }

            delete();
        }
    }

    private void renameTagInNotes(String tagOld, String tagNew, Bucket<Note> notesBucket) {
        ObjectCursor<Note> notesWithOldTag = findNotes(notesBucket, tagOld);

        while (notesWithOldTag.moveToNext()) {
            Note note = notesWithOldTag.getObject();
            TagUtils.renameTagInNote(note, tagOld, tagNew);
            note.save();
        }

        notesWithOldTag.close();
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