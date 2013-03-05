package com.automattic.simplenote.models;

public class Tag {
	protected String simperiumKey;
	protected int index;
	
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
	
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
}