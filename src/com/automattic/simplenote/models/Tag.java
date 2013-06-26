package com.automattic.simplenote.models;

import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;

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
	}

	public Tag(String key, Map<String,Object>properties){
		super(key, properties);
		setName((String) properties.get("name"));
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (name == null) {
			this.name = "";
		} else {
			this.name = name;
		}
	}
		
	public int getTagIndex() {
		return tagIndex;
	}
	
	public void setTagIndex(int tagIndex) {
		this.tagIndex = tagIndex;
	}
}