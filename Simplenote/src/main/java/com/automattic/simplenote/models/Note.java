package com.automattic.simplenote.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.automattic.simplenote.R;

import com.simperium.client.Bucket;
import com.simperium.client.Query;
import com.simperium.client.Query.ComparisonType;
import com.simperium.client.Query.SortType;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.BucketSchema.Index;
import com.simperium.client.BucketSchema.Indexer;

public class Note extends BucketObject {
	
	public static final String BUCKET_NAME="note";
    public static final String PINNED_TAG="pinned";
    public static final String NEW_LINE="\n";
    
    private static final String CONTENT_CONCAT_FORMAT="%s %s";
    private static final String BLANK_CONTENT="";
    private static final String SPACE = " ";
    
    public static final String CONTENT_PROPERTY="content";
    public static final String TAGS_PROPERTY="tags";
    public static final String SYSTEM_TAGS_PROPERTY="systemTags";
    public static final String CREATION_DATE_PROPERTY="creationDate";
    public static final String MODIFICATION_DATE_PROPERTY="modificationDate";
    public static final String SHARE_URL_PROPERTY="shareURL";
    public static final String PUBLISH_URL_PROPERTY="publishURL";
    public static final String DELETED_PROPERTY="deleted";
    public static final String TITLE_INDEX_NAME="title";
    public static final String CONTENT_PREVIEW_INDEX_NAME="contentPreview";
    public static final String PINNED_INDEX_NAME="pinned";
	
	protected String title = null;
	protected String contentPreview = null;

	public static class Schema extends BucketSchema<Note> {

        public Schema(){
            autoIndex();
            addIndex(noteIndexer);
            setDefault(CONTENT_PROPERTY, "");
            setDefault(SYSTEM_TAGS_PROPERTY, new ArrayList<Object>());
            setDefault(TAGS_PROPERTY, new ArrayList<Object>());
            setDefault(DELETED_PROPERTY, false);
            setDefault(SHARE_URL_PROPERTY, "");
            setDefault(PUBLISH_URL_PROPERTY, "");
        }

        private Indexer noteIndexer = new Indexer<Note>(){
            @Override
            public List<Index> index(Note note){
                List<Index> indexes = new ArrayList<Index>();
                indexes.add(new Index(PINNED_INDEX_NAME, note.isPinned()));
                indexes.add(new Index(CONTENT_PREVIEW_INDEX_NAME, note.getContentPreview()));
                indexes.add(new Index(TITLE_INDEX_NAME, note.getTitle()));
                return indexes;
            }
        };

        public String getRemoteName(){
            return Note.BUCKET_NAME;
        }

		public Note build(String key, Map<String,Object>properties){
			Note note = new Note(key, properties);
			return note;
		}

        public void update(Note note, Map<String,Object>properties){
            note.properties = properties;
            note.title = null;
            note.contentPreview = null;
        }
	}
    
    public static Query<Note> all(Bucket<Note> noteBucket){
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.NOT_EQUAL_TO, true);
    }

    public static Query<Note> allDeleted(Bucket<Note> noteBucket){
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.EQUAL_TO, true);
    }

    public static Query<Note> search(Bucket<Note> noteBucket, String searchString){
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.NOT_EQUAL_TO, true)
                .where(CONTENT_PROPERTY, ComparisonType.LIKE, "%" + searchString + "%");
    }

    public static Query<Note> allInTag(Bucket<Note> noteBucket, String tag){
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.NOT_EQUAL_TO, true)
                .where(TAGS_PROPERTY, ComparisonType.LIKE, tag);
    }


    public Note(String key){
        super(key, new HashMap<String,Object>());
    }

	public Note(String key, Map<String,Object>properties) {
		super(key, properties);
	}

    protected void updateTitleAndPreview(){
        // try to bulid a title and preview property out of content
        String content = getContent();
        // title = "Hello World";
        // contentPreview = "This is a preview";
        int location = -1;
        int lines = 0;
        int content_lines = 0;
        int start = 0;
        boolean foundTitle = false;
        boolean foundContent = false;
        // loop until we've found all the new lines
        do {
            start = location + 1;
            location = content.indexOf(NEW_LINE, start);
            if (location > start + 1) {
                String possible = content.substring(start, location);
                if (!foundTitle) {
                    foundTitle = true;
                    title = possible;
                }
            }
            lines ++;
        } while(location >= 0 && lines < 5 && !foundTitle);
        lines = 0;
        contentPreview = "";
        do {
            start = location + 1;
            location = content.indexOf(NEW_LINE, start);
            if (location > start + 1) {
                String possible = content.substring(start, location);
                contentPreview = String.format(CONTENT_CONCAT_FORMAT, contentPreview, possible);
                if (contentPreview.length() >= 300) {
                    foundContent = true;
                    contentPreview = contentPreview.substring(0, 300);
                }
            }
            lines ++;
        } while(location >= 0 && !foundContent);
        if (!foundTitle) {
            title = content;
        }
        if (contentPreview.isEmpty()) {
            contentPreview = content;
        } else {
            contentPreview = contentPreview.trim();
        }
    }
	
	public String getTitle() {
        if (title == null) {
            updateTitleAndPreview();
        }
		return title;
	}
    
    public String getTitle(String ifBlank){
        if (title == null) {
            updateTitleAndPreview();
        }
        if (title.trim().equals("")) {
            return ifBlank;
        } else {
            return title;
        }
    }

	public String getContent() {
        Object content = getProperty(CONTENT_PROPERTY);
        if (content == null) {
            return BLANK_CONTENT;
        }
        return (String) content;
	}

	public void setContent(String content) {
        title = null;
        contentPreview = null;
        setProperty(CONTENT_PROPERTY, content);
	}
	
	public String getContentPreview() {
        if (contentPreview == null) {
            updateTitleAndPreview();
        }
		return contentPreview;
	}

    public String getContentPreview(int lines){
        if (contentPreview == null) {
            updateTitleAndPreview();
        }
        return contentPreview;
    }

	public Calendar getCreationDate() {
        return numberToDate((Number)getProperty(CREATION_DATE_PROPERTY));
	}

	public void setCreationDate(Calendar creationDate) {
        setProperty(CREATION_DATE_PROPERTY, creationDate.getTimeInMillis()/1000);
	}

	public Calendar getModificationDate() {
        return numberToDate((Number)getProperty(MODIFICATION_DATE_PROPERTY));
	}

	public void setModificationDate(Calendar modificationDate) {
        setProperty(MODIFICATION_DATE_PROPERTY, modificationDate.getTimeInMillis()/1000);
	}

    public boolean hasTag(String noteTagKey){
        List<String> tags = getTags();
        for (String tagKey : tags) {
            if (noteTagKey.equals(tagKey)) return true;
        }
        return false;
    }

	public List<String> getTags() {
        Object tags = getProperty(TAGS_PROPERTY);
        if (tags == null) {
            tags = new ArrayList<String>();
            setProperty(TAGS_PROPERTY, tags);
        }
        return (ArrayList<String>) tags;
	}

	public void setTags(List<String> tags) {
        setProperty(TAGS_PROPERTY, tags);
	}

    /**
     * String of tags delimited by a space
     */
    public CharSequence getTagString(){
        StringBuilder tagString = new StringBuilder();
        List<String> tags = getTags();
        for(String tag : tags){
            if (tagString.length() > 0) {
                tagString.append(SPACE);
            }
            tagString.append(tag);
        }
        return tagString;
    }

    /**
     * Sets the note's tags by providing it with a {@link String} of space
     * seperated tags. Filters out duplicate tags.
     * 
     * @param tagString a space delimited list of tags
     */
    public void setTagString(String tagString){
        List<String> tags = getTags();
        tags.clear();
        if (tagString == null) return;
        // Make sure string has a trailing space
        if (tagString.length() > 1 && !tagString.substring(tagString.length() - 1).equals(SPACE))
            tagString = tagString + SPACE;
        // for comparing case-insensitive strings, would like to find a way to
        // do this without allocating a new list and strings
        List<String> tagsUpperCase = new ArrayList<String>();
        // remove all current tags
        int start = 0;
        int next = -1;
        String possible;
        String possibleUpperCase;
        // search tag string for space characters and pull out individual tags
        do {
            next = tagString.indexOf(SPACE, start);
            if (next > start) {
                possible = tagString.substring(start, next);
                possibleUpperCase = possible.toUpperCase();
                if (!possible.equals(SPACE) && !tagsUpperCase.contains(possibleUpperCase)) {
                    tagsUpperCase.add(possibleUpperCase);
                    tags.add(possible);
                }
            }
            start = next + 1;
        } while(next > -1);
    }

	public List<String> getSystemTags() {
        Object tags = getProperty(SYSTEM_TAGS_PROPERTY);
        if (tags == null) {
            return new ArrayList<String>();
        }
        return (List<String>) tags;
	}

	public Boolean isDeleted() {
        Object deleted = getProperty(DELETED_PROPERTY);
        if (deleted == null) {
            return false;
        }
		if (deleted instanceof Boolean) {
			return (Boolean) deleted;
		} else if (deleted instanceof Number) {
			return ((Number)deleted).intValue() == 0 ? false : true;
        } else {
            return false;
        }
	}

	public void setDeleted(boolean deleted) {
		setProperty(DELETED_PROPERTY, deleted);
	}

	public boolean isPinned() {
        return getSystemTags().contains(PINNED_TAG);
	}

	public void setPinned(boolean isPinned) {
        if (isPinned && !isPinned()) {
            getSystemTags().add(PINNED_TAG);
        } else if (!isPinned && isPinned()){
            getSystemTags().remove(PINNED_TAG);
        }
	}

    public static String dateString(Number time, boolean useShortFormat, Context context){
        Calendar c = numberToDate(time);
        return dateString(c, useShortFormat, context);
    }

	public static String dateString(Calendar c, boolean useShortFormat, Context context) {
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
			date = context.getResources().getString(R.string.today);
			if (useShortFormat)
				retVal = time;
			else
				retVal = date + ", " + time;
		} else if ((year == diff.get(Calendar.YEAR)) && (month == diff.get(Calendar.MONTH)) && (day == 1)) {
			date = context.getResources().getString(R.string.yesterday);
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

    protected Object getProperty(String key){
        return properties.get(key);
    }

    protected void setProperty(String key, Object value){
        properties.put(key, value);
    }

    public static Calendar numberToDate(Number time){
        Calendar date = Calendar.getInstance();
        if (time != null) {
            date.setTimeInMillis(time.longValue()*1000);
        }
        return date;
    }

    /**
     * Check if the note has any changes
     * @param content the new note content
     * @param tagString space separated tags
     * @param isPinned note is pinned
     * @return true if note has changes, false if it is unchanged.
     */
    public boolean hasChanges(String content, String tagString, boolean isPinned) {

        if (content.equals(this.getContent()) && this.isPinned() == isPinned && tagString.equals(this.getTagString().toString()))
            return false;
        else
            return true;
    }
}
