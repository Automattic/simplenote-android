package com.automattic.simplenote.models;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;
import com.simperium.client.Query.ComparisonType;
import com.simperium.client.Query.SortType;

import java.util.Map;


public class Tag extends BucketObject {
	
	public static final String BUCKET_NAME="tag";
	protected int tagIndex;
	protected String name = "";

	public static class Schema extends BucketSchema<Tag> {

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
        return bucket.query().order("name", SortType.ASCENDING);
    }

	public Tag(String key, Map<String,Object>properties){
		super(key, properties);
	}

	public String getName() {
		return (String) properties.get("name");
	}
	
	public void setName(String name) {
		if (name == null) {
			name = "";
		}
        properties.put("name", name);
	}
		
	public Integer getIndex() {
        return (Integer) properties.get("index");
	}
	
	public void setIndex(Integer tagIndex) {
        if (tagIndex == null) {
            properties.remove("index");
        } else {
            properties.put("index", tagIndex);
        }
	}

    protected void setProperties(Map<String,Object> properties){
        this.properties = properties;
    }
}