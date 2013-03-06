package com.automattic.simplenote.models;

import com.simperium.client.Bucket;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;

import java.util.Map;

public class Note extends Bucket.Object {
	
	public static final String BUCKET_NAME="note";
	
	protected String simperiumKey;

	protected String title;
	protected String content;
	protected String contentPreview;
	protected Calendar creationDate;
	protected Calendar modificationDate;
	protected Vector<String> tags;
	protected Vector<String> systemTags;
	protected boolean deleted;
	protected boolean pinned;
	protected int lastPosition;
	protected String shareURL;

	public static class Schema extends Bucket.Schema<Note> {
		public Note build(String key, Map<String,Object>properties){
			return new Note(key, properties);
		}
	}

	public Note(String key, Map<String,Object>properties) {
		super(key, properties);
		
		content = "Testing!";
		tags = new Vector<String>();
		systemTags = new Vector<String>();
		creationDate = Calendar.getInstance();
		modificationDate = Calendar.getInstance();
	}
	
    public Map<String, java.lang.Object> getDiffableValue() {
    	properties.put("content", content);
    	properties.put("tags", tags);
    	properties.put("systemTags", systemTags);
    	properties.put("deleted", deleted);
    	properties.put("pinned", pinned);
    	properties.put("creationDate", creationDate);
    	properties.put("modificationDate", modificationDate);
    	properties.put("shareURL", shareURL);
        return properties;
    }

	public String getSimperiumKey() {
		return simperiumKey;
	}

	public void setSimperiumKey(String simperiumKey) {
		this.simperiumKey = simperiumKey;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		String thisLine;
		String[] lines = content.split("\n");
		boolean foundTitle, foundContent;
		foundTitle = foundContent = false;

		for (int i = 0; i < lines.length; i++) {
			thisLine = lines[i];
			if (!foundContent) {
				if (thisLine != null) {
					if (!foundTitle) {
						setTitle(thisLine);
						foundTitle = true;
					} else {
						this.contentPreview = thisLine;
						foundContent = true;
					}
				}
			} else {
				this.contentPreview += thisLine;
			}
		}
		this.content = content;
	}
	
	public String getContentPreview() {
		return contentPreview;
	}

	public void setContentPreview(String contentPreview) {
		this.contentPreview = contentPreview;
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

	public int getLastPosition() {
		return lastPosition;
	}

	public void setLastPosition(int lastPosition) {
		this.lastPosition = lastPosition;
	}

	public String getShareURL() {
		return shareURL;
	}

	public void setShareURL(String shareURL) {
		this.shareURL = shareURL;
	}

	public static String dateString(Calendar c, boolean useShortFormat) {
		int year, month, day;

		String time, date, retVal;
		time = date = "";

		Calendar diff = Calendar.getInstance();
		diff.setTimeInMillis(diff.getTimeInMillis() - c.getTimeInMillis());

		year = diff.get(Calendar.YEAR);
		month = diff.get(Calendar.MONTH);
		day = diff.get(Calendar.DAY_OF_MONTH);

		diff.setTimeInMillis(0); // starting time
		time = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());
		if ((year == diff.get(Calendar.YEAR)) && (month == diff.get(Calendar.MONTH)) && (day == diff.get(Calendar.DAY_OF_MONTH))) {
			date = "Today";
			if (useShortFormat)
				retVal = time;
			else
				retVal = date + ", " + time;
		} else if ((year == diff.get(Calendar.YEAR)) && (month == diff.get(Calendar.MONTH)) && (day == 1)) {
			date = "Yesterday";
			if (useShortFormat)
				retVal = date;
			else
				retVal = date + ", " + time;
		} else {
			date = new SimpleDateFormat("MMM dd", Locale.US).format(c.getTime());
			retVal = date + ", " + time;
		}

		return retVal;
	}
}