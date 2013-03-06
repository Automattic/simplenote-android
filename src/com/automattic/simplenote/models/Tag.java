package com.automattic.simplenote.models;

import com.simperium.client.Bucket;

import java.util.Map;


public class Tag extends Bucket.Object {
	
	public static final String BUCKET_NAME="tag";
	
	protected String simperiumKey;
	protected int tagIndex;
	
	public static class Schema extends Bucket.Schema<Tag> {
		public Tag build(String key, Integer version, Map<String,Object>properties){
			return new Tag(key, version, properties);
		}
	}
	
	public Tag(String key, Integer version, Map<String,Object>properties){
		super(key, version, properties);
	}
	
	// Map "name" to simperimKey for convenience (they could one day be different)
	public String getName() {
		return simperiumKey;
	}
	
	public void setName(String name) {
		this.simperiumKey = name;
	}
	
	public String getSimperiumKey() {
		return simperiumKey;
	}
	
	public void setSimperiumKey(String simperiumKey) {
		this.simperiumKey = simperiumKey;
	}
	
	public int getTagIndex() {
		return tagIndex;
	}
	
	public void setTagIndex(int tagIndex) {
		this.tagIndex = tagIndex;
	}
}