package com.automattic.simplenote;

import org.json.JSONArray;

import java.util.Calendar;
import java.util.Vector;
import java.util.List;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;

import com.simperium.client.StorageProvider;
import com.simperium.client.Bucket;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

import java.util.Calendar;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;

import android.util.Log;

public class NoteDB {

	private static final int DATABASE_VERSION = 2;
	private SQLiteDatabase db;
	private static final String DATABASE_NAME = "simplenote";

	private static final String CREATE_TABLE_NOTES = "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT, simperiumKey TEXT, title TEXT, content TEXT, contentPreview TEXT, creationDate DATE, modificationDate DATE, deleted BOOLEAN, lastPosition INTEGER, pinned BOOLEAN, shareURL TEXT, systemTags TEXT, tags TEXT);";
	private static final String ADD_NOTES_INDEX = "CREATE INDEX simperiumKeyNotesIndex ON notes(simperiumKey);";

	private static final String CREATE_TABLE_TAGS = "CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT, tagIndex INTEGER, simperiumKey TEXT, name TEXT);";
	private static final String ADD_TAGS_INDEX = "CREATE INDEX simperiumKeyTagsIndex ON tags(simperiumKey);";
	
	private static final String CREATE_TABLE_BUCKETS = "CREATE TABLE IF NOT EXISTS buckets (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, cv TEXT);";

	private static final String NOTES_TABLE = "notes";
	private static final String TAGS_TABLE = "tags";

	public NoteDB(Context ctx) {

		db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

		db.execSQL(CREATE_TABLE_NOTES);
		db.execSQL(CREATE_TABLE_TAGS);

		if (db.getVersion() < 1) {
			// Create indexes for new install
			db.execSQL(ADD_NOTES_INDEX);
			db.execSQL(ADD_TAGS_INDEX);

			// Test notes!
			/*for (int i = 0; i < 100; i++) {
				ContentValues values = new ContentValues();
				values.put("simperiumKey", String.valueOf(i));
				values.put("title", "example title #" + i);

				if (i % 2 == 0) {
					values.put("content", "Wow, would you look at that, it's note #" + String.valueOf(i) + "!" + "\n"
							+ "Here's some more content for this note.");
					values.put("contentPreview", "Wow, would you look at that, it's note #" + String.valueOf(i) + "!" + "\n"
							+ "Here's some more content for this note.");
				} else {
					values.put("content", "I'm just a simple note. A Simplenote, get it?");
					values.put("contentPreview", "I'm just a simple note. A Simplenote, get it?");
				}
				values.put("creationDate", Calendar.getInstance().getTimeInMillis());
				values.put("modificationDate", Calendar.getInstance().getTimeInMillis() - (i * 1000));
				values.put("deleted", false);
				values.put("lastPosition", 0);

				if (i == 20 || i == 30) {
					values.put("pinned", true);
				} else {
					values.put("pinned", false);
				}

				values.put("shareURL", "url");
				values.put("systemTags", "");
				values.put("tags", "");

				db.insert(NOTES_TABLE, null, values);
			}*/

		}
		if (db.getVersion() < 2) {
			db.execSQL(CREATE_TABLE_BUCKETS);
		}

		db.setVersion(DATABASE_VERSION);

	}
	
	boolean create(Note note) {
		if (note == null)
			return false;

		ContentValues values = new ContentValues();
		values.put("simperiumKey", note.getSimperiumKey());
		values.put("title", note.getTitle());
		values.put("content", note.getContent());
		values.put("contentPreview", note.getContentPreview());
		values.put("creationDate", note.getCreationDate().getTimeInMillis());
		values.put("modificationDate", note.getModificationDate().getTimeInMillis());
		values.put("deleted", note.isDeleted());
		values.put("lastPosition", note.getLastPosition());
		values.put("pinned", note.isPinned());
		values.put("shareURL", note.getShareURL());
		values.put("systemTags", new JSONArray(note.getSystemTags()).toString());
		values.put("tags", new JSONArray(note.getTags()).toString());

		return db.insert(NOTES_TABLE, null, values) >= 0;
	}

	boolean create(Tag tag) {
		if (tag == null)
			return false;

		ContentValues values = new ContentValues();
		values.put("simperiumKey", tag.getSimperiumKey());
		values.put("index", tag.getTagIndex());
		values.put("name", tag.getName());

		return db.insert(TAGS_TABLE, null, values) >= 0;
	}

	boolean update(Note note) {
		if (note == null)
			return false;

		ContentValues values = new ContentValues();
		values.put("simperiumKey", note.getSimperiumKey());
		values.put("title", note.getTitle());
		values.put("content", note.getContent());
		values.put("contentPreview", note.getContentPreview());
		values.put("creationDate", note.getCreationDate().getTimeInMillis());
		values.put("modificationDate", note.getModificationDate().getTimeInMillis());
		values.put("deleted", note.isDeleted());
		values.put("lastPosition", note.getLastPosition());
		values.put("pinned", note.isPinned());
		values.put("shareURL", note.getShareURL());
		values.put("systemTags", new JSONArray(note.getSystemTags()).toString());
		values.put("tags", new JSONArray(note.getTags()).toString());

		return db.update(NOTES_TABLE, values, "simperiumKey=" + note.getSimperiumKey(), null) > 0;
	}

	boolean update(Tag tag) {
		if (tag == null)
			return false;

		ContentValues values = new ContentValues();
		values.put("simperiumKey", tag.getSimperiumKey());
		values.put("index", tag.getTagIndex());
		values.put("name", tag.getName());

		return db.update(TAGS_TABLE, values, "simperiumKey=" + tag.getSimperiumKey(), null) > 0;
	}

	boolean delete(Note note) {
		if (note == null)
			return false;

		return db.delete(NOTES_TABLE, "simperiumKey=" + note.getSimperiumKey(), null) > 0;
	}

	boolean delete(Tag tag) {
		if (tag == null)
			return false;

		return db.delete(TAGS_TABLE, "simperiumKey=" + tag.getSimperiumKey(), null) > 0;
	}

	public Cursor fetchAllNotes(Context context) {

		// Get sort preference
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		int sortPref = Integer.parseInt(sharedPref.getString("pref_key_sort_order", "0"));
		String orderBy = "modificationDate DESC";

		switch (sortPref) {
		case 1:
			orderBy = "creationDate DESC";
			break;
		case 2:
			orderBy = "content ASC";
			break;
		case 3:
			orderBy = "modificationDate ASC";
			break;
		case 4:
			orderBy = "creationDate ASC";
			break;
		case 5:
			orderBy = "content DESC";
			break;
		}

		Cursor cursor = db.query(NOTES_TABLE, new String[] { "rowid _id", "simperiumKey", "title", "content", "contentPreview",
				"creationDate", "modificationDate", "deleted", "lastPosition", "pinned", "shareURL", "systemTags", "tags" }, null, null,
				null, null, orderBy);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor searchNotes(String searchString) {
		Cursor cursor = db.query(NOTES_TABLE, new String[] { "rowid _id", "simperiumKey", "title", "content", "contentPreview",
				"creationDate", "modificationDate", "deleted", "lastPosition", "pinned", "shareURL", "systemTags", "tags" },
				"content like " + "'%" + searchString + "%'", null, null, null, "PINNED DESC");
		if (cursor != null) {
			cursor.moveToFirst();
		}

		return cursor;
	}
	
	public SimperiumStore getSimperiumStore(){
		return new SimperiumStore();
	}
	public static final String TAG = "Simplenote";
	private class SimperiumStore implements StorageProvider {
	    /**
	     * Store the change version for a bucket
	     */
	    public String getChangeVersion(Bucket bucket){
	    	return null;
	    }
	    public void setChangeVersion(Bucket bucket, String version){
	    	
	    }
	    public Boolean hasChangeVersion(Bucket bucket){
			return false;
	    	
	    }
	    public Boolean hasChangeVersion(Bucket bucket, String version){
	    	return false;
	    }
	    /**
	     * Store bucket object data
	     */
	    public void addObject(Bucket bucket, String key, Bucket.Syncable object){
	    	Log.d(TAG, String.format("Time to save %s in %s", key, bucket.getName()));
	    }
	    public void updateObject(Bucket bucket, String key, Bucket.Syncable object){
	    	
	    }
	    public void removeObject(Bucket bucket, String key){
	    	
	    }
	    /**
	     * Retrieve entities and details
	     */
	    public <T extends Bucket.Syncable> T getObject(Bucket<T> bucket, String key){
	    	return null;
	    }
	    public Boolean containsKey(Bucket bucket, String key){
	    	return false;
	    }
	    public Boolean hasKeyVersion(Bucket bucket, String key, Integer version){
	    	return false;
	    }
	    public Integer getKeyVersion(Bucket bucket, String key){
	    	return null;
	    }
	    public <T extends Bucket.Syncable> List<T> allEntities(Bucket<T> bucket){
	    	return null;
	    }
		
	}

}
