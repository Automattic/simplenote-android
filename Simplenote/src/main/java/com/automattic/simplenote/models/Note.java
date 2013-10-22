package com.automattic.simplenote.models;

import android.content.Context;

import com.automattic.simplenote.R;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;
import com.simperium.client.Query.ComparisonType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
    public static final String MODIFIED_INDEX_NAME="modified";
    public static final String CREATED_INDEX_NAME="created";

    static public final String[] FULL_TEXT_INDEXES = new String[]{
        Note.TITLE_INDEX_NAME, Note.CONTENT_PROPERTY };
	
	protected String title = null;
	protected String contentPreview = null;


	public static class Schema extends BucketSchema<Note> {

        protected static NoteIndexer sNoteIndexer = new NoteIndexer();
        protected static NoteFullTextIndexer sFullTextIndexer = new NoteFullTextIndexer();

        public Schema(){
            autoIndex();
            addIndex(sNoteIndexer);
            setupFullTextIndex(sFullTextIndexer, NoteFullTextIndexer.INDEXES);
            setDefault(CONTENT_PROPERTY, "");
            setDefault(SYSTEM_TAGS_PROPERTY, new JSONArray());
            setDefault(TAGS_PROPERTY, new JSONArray());
            setDefault(DELETED_PROPERTY, false);
            setDefault(SHARE_URL_PROPERTY, "");
            setDefault(PUBLISH_URL_PROPERTY, "");
        }

        public String getRemoteName(){
            return Note.BUCKET_NAME;
        }

        public Note build(String key, JSONObject properties) {
            Note note = new Note(key, properties);
            return note;
        }

        public void update(Note note, JSONObject properties) {
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


    public Note(String key) {
        super(key, new JSONObject());
    }

    public Note(String key, JSONObject properties) {
        super(key, properties);
    }

    protected void updateTitleAndPreview(){
        // try to build a title and preview property out of content
        String content = getContent().trim();
        // title = "Hello World";
        // contentPreview = "This is a preview";

        int firstNewLinePosition = content.indexOf(NEW_LINE);
        if (firstNewLinePosition > -1 && firstNewLinePosition < 200) {
            title = content.substring(0, firstNewLinePosition).trim();

            if (firstNewLinePosition < content.length()) {
                contentPreview = content.substring(firstNewLinePosition, content.length());
                contentPreview = contentPreview.replace(NEW_LINE, SPACE).replace(SPACE+SPACE, SPACE).trim();
                if (contentPreview.length() >= 300) {
                    contentPreview = contentPreview.substring(0, 300);
                }
            }
            else {
                contentPreview = content;
            }
        }
        else {
            title = content;
            contentPreview = content;
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

    public boolean hasTag(String tag){
        List<String> tags = getTags();
        String tagLower = tag.toLowerCase();
        for (String tagName : tags) {
            if (tagLower.equals(tagName.toLowerCase())) return true;
        }
        return false;
    }

    public boolean hasTag(Tag tag){
        return hasTag(tag.getSimperiumKey());
    }

    public List<String> getTags() {

        JSONArray tags = (JSONArray) getProperty(TAGS_PROPERTY);

        if (tags == null) {
            tags = new JSONArray();
            setProperty(TAGS_PROPERTY, tags);
        }

        int length = tags.length();

        List<String> tagList = new ArrayList<String>(length);

        if (length == 0) return tagList;

        for (int i=0; i<length; i++) {
            String tag = tags.optString(i);
            if (!tag.equals(""))
                tagList.add(tag);
        }

        return tagList;
    }

    public void setTags(List<String> tags) {
        setProperty(TAGS_PROPERTY, new JSONArray(tags));
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

        if (tagString == null) {
            setTags(tags);
            return;
        }

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
        setTags(tags);
    }

    public JSONArray getSystemTags() {
        JSONArray tags = (JSONArray) getProperty(SYSTEM_TAGS_PROPERTY);
        if (tags == null) {
            tags = new JSONArray();
            setProperty(SYSTEM_TAGS_PROPERTY, tags);
        }
        return tags;
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
        JSONArray tags = getSystemTags();
        int length = tags.length();
        for (int i=0; i<length; i++) {
            if (tags.optString(i).equals(PINNED_TAG))
                return true;
        }
        return false;
    }

    public void setPinned(boolean isPinned) {
        if (isPinned && !isPinned()) {
            getSystemTags().put(PINNED_TAG);
        } else if (!isPinned && isPinned()){
            JSONArray tags = getSystemTags();
            JSONArray newTags = new JSONArray();
            int length = tags.length();
            try {
                for (int i=0; i<length; i++) {
                    Object val = tags.get(i);
                    if (val.equals(PINNED_TAG))
                        newTags.put(val);
                }
            } catch (JSONException e) {
                // could not update pinned setting
            }
            setProperty(SYSTEM_TAGS_PROPERTY, newTags);
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

    public static Calendar numberToDate(Number time){
        Calendar date = Calendar.getInstance();
        if (time != null) {
            // Flick Note uses millisecond resolution timestamps Simplenote expects seconds
            // since we only deal with create and modify timestamps, they should all have occured
            // at the present time or in the past.
            float now = date.getTimeInMillis()/1000;
            float magnitude = time.floatValue()/now;
            if (magnitude >= 2.f) time = time.longValue()/1000;
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
