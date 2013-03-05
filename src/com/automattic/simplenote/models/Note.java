package com.automattic.simplenote.models;

import java.sql.Date;
import java.util.Calendar;
import java.util.Vector;

public class Note {
	protected String simperiumKey;
	protected String content;
	protected Calendar creationDate;
	protected Calendar modificationDate;
	protected Vector<String> tags;
	protected Vector<String> systemTags;
	protected boolean deleted;
	protected boolean pinned;
		    
    public Note()
    {
    }

	public String getSimperiumKey() {
		return simperiumKey;
	}

	public void setSimperiumKey(String simperiumKey) {
		this.simperiumKey = simperiumKey;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Calendar getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Calendar creationDate) {
		this.creationDate = creationDate;
	}

	public Calendar getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Calendar modificationDate) {
		this.modificationDate = modificationDate;
	}

	public Vector<String> getTags() {
		return tags;
	}

	public void setTags(Vector<String> tags) {
		this.tags = tags;
	}

	public Vector<String> getSystemTags() {
		return systemTags;
	}

	public void setSystemTags(Vector<String> systemTags) {
		this.systemTags = systemTags;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isPinned() {
		return pinned;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}	
}
