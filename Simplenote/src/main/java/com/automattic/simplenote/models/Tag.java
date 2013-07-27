package com.automattic.simplenote.models;

import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;
import com.simperium.client.Query.ComparisonType;
import com.simperium.client.Query.SortType;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import android.util.Log;

public class Tag extends BucketObject {
	
	public static final String BUCKET_NAME="tag";
    public static final String NOTE_COUNT_INDEX_NAME = "note_count";
    public static final String NAME_PROPERTY = "name";
    public static final String INDEX_PROPERTY = "index";
    protected int tagIndex;
	protected String name = "";

    public static class Schema extends BucketSchema<Tag> {

        public Schema(){
            autoIndex();
        }

        public String getRemoteName(){
            return Tag.BUCKET_NAME;
        }

		public Tag build(String key, Map<String,Object>properties){
			return new Tag(key, properties);
		}

        public void update(Tag tag, Map<String,Object>properties){
            tag.setProperties(properties);
        }
	}

    public static Query<Tag> all(Bucket<Tag> bucket){
        return bucket.query().order(INDEX_PROPERTY).orderByKey();
    }

    public static Query<Tag> allWithCount(Bucket<Tag> bucket) {
        return all(bucket).include(NAME_PROPERTY, NOTE_COUNT_INDEX_NAME);
    }

    public Tag(String key){
        super(key, new HashMap<String,Object>());
    }

	public Tag(String key, Map<String,Object>properties){
		super(key, properties);
	}

	public String getName() {
		String name = (String) properties.get(NAME_PROPERTY);
        if (name == null) {
            name = getSimperiumKey();
        }
        return name;
	}
	
	public void setName(String name) {
		if (name == null) {
			name = "";
		}
        properties.put(NAME_PROPERTY, name);
	}
		
	public Integer getIndex() {
        return (Integer) properties.get(INDEX_PROPERTY);
	}
	
	public void setIndex(Integer tagIndex) {
        if (tagIndex == null) {
            properties.remove("index");
        } else {
            properties.put("index", tagIndex);
        }
	}

    public void renameTo(String name, Bucket<Note> notesBucket){
        String key = name.toLowerCase();
        if (!getSimperiumKey().equals(key)) {
            // create a new tag with the value as the key/name
            // TODO: tag already exists?
            Tag newTag = ((Bucket<Tag>) getBucket()).newObject(key);
            newTag.setName(name);
            newTag.save();
            // get all the notes from tag, remove the item
            ObjectCursor<Note> notesCursor = findNotes(notesBucket);
            while(notesCursor.moveToNext()){
                Note note = notesCursor.getObject();
                List<String> tags = new ArrayList<String>(note.getTags());
                List<String> newTags = new ArrayList<String>(tags.size());
                // iterate and do a case insensitive comparison on each tag
                for (String tag : tags) {
                    // if it's this tag, add the new tag
                    if (tag.toLowerCase().equals(getSimperiumKey())) {
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
        } else if(!getName().equals(name)) {
            setName(name);
            save();
        }
    }

    protected void setProperties(Map<String,Object> properties){
        this.properties = properties;
    }

    public ObjectCursor<Note> findNotes(Bucket<Note> notesBucket){
        return notesBucket.query().where("tags", ComparisonType.LIKE, getSimperiumKey()).execute();
    }
}