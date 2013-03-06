package com.automattic.simplenote.models;

import com.simperium.client.Bucket;

import java.util.Map;


public class Tag extends Bucket.Object {
	
	public static final String BUCKET_NAME="tag";
	
	protected String simperiumKey;
	protected int tagIndex;
	
	public static class Schema extends Bucket.Schema<Tag> {
		public Tag build(String key, Map<String,Object>properties){
			return new Tag(key, properties);
		}
	}
	
	public Tag(String key, Map<String,Object>properties){
		super(key, properties);
	}
	
	// Map "name" to simperimKey for convenience (they could one day be different)
	public String getName() {
		return simperiumKey;
	}
	
	public void setName(String name) {
		this.simperiumKey = name;
	}
		
	public int getTagIndex() {
		return tagIndex;
	}
	
	public void setTagIndex(int tagIndex) {
		this.tagIndex = tagIndex;
	}
}