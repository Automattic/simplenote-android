package com.automattic.simplenote.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import android.util.Log;

import com.simperium.client.Bucket;

public class Note extends Bucket.Object {
	
	public static final String BUCKET_NAME="note";
	
	protected String title;
	protected String content;
	protected String contentPreview;
	protected Calendar creationDate;
	protected Calendar modificationDate;
	protected ArrayList<String> tags;
	protected ArrayList<String> systemTags;
	protected boolean deleted;
	protected boolean pinned;
	protected int lastPosition;
	protected String shareURL;
	protected String publishURL;

	public static class Schema extends Bucket.Schema<Note> {
		public Note build(String key, Map<String,Object>properties){
			Note note = new Note(key, properties);
			return note;
		}
	}

	public Note(String key, Map<String,Object>properties) {
		super(key, properties);
		
		content = (String)properties.get("content");
		if (content == null)
			content = "";
		
		setDeleted(properties.get("deleted"));
		
		setTags((ArrayList<String>)properties.get("tags"));
		if (tags == null)
			tags = new ArrayList<String>();

		setSystemTags((ArrayList<String>)properties.get("systemTags"));
		if (systemTags == null)
			systemTags = new ArrayList<String>();

		creationDate = Calendar.getInstance();
		Number creationProp = (Number)properties.get("creationDate");
		if (creationProp != null) {
			creationDate.setTimeInMillis(creationProp.longValue()*1000);
		}
		
		modificationDate = Calendar.getInstance();
		Number modificationProp = (Number)properties.get("modificationDate");
		if (modificationProp != null) {
			modificationDate.setTimeInMillis(modificationProp.longValue()*1000);
		}

		shareURL = (String)properties.get("shareURL");
		if (shareURL == null)
			shareURL = "";
		
		publishURL = (String)properties.get("publishURL");
		if (publishURL == null)
			publishURL = "";
	}
	
    public Map<String, Object> getDiffableValue() {
		Map<String, Object> properties = new HashMap<String,Object>();
    	properties.put("content", content);
    	properties.put("tags", tags);
    	properties.put("systemTags", systemTags);
    	properties.put("deleted", deleted);
    	properties.put("creationDate", creationDate.getTimeInMillis()/1000);
    	properties.put("modificationDate", modificationDate.getTimeInMillis()/1000);
    	properties.put("shareURL", shareURL);
    	properties.put("publishURL", publishURL);
        return properties;
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

	public ArrayList<String> getTags() {
		return tags;
	}

	public void setTags(ArrayList<String> tags) {
		this.tags = tags;
	}

	public ArrayList<String> getSystemTags() {
		return systemTags;
	}

	public void setSystemTags(ArrayList<String> systemTags) {
		if (systemTags == null) {
			systemTags = new ArrayList();
		}
		this.systemTags = systemTags;
		pinned = systemTags.contains("pinned");
	}

	public boolean isDeleted() {
		return deleted;
	}
	
	public void setDeleted(Object deleted) {
		if (deleted != null) {
			if (deleted instanceof Boolean) {
				this.deleted = ((Boolean)deleted).booleanValue();
			} else if (deleted instanceof Number) {
				this.deleted = ((Number)deleted).intValue() == 0 ? false : true;
			}
		}
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public void setDeleted(Number deleted) {
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
