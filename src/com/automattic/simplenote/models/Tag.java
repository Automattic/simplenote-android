package com.automattic.simplenote.models;

public class Tag {
	protected String simperiumKey;
	protected int tagIndex;
	
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