package com.automattic.simplenote;

import org.json.JSONArray;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;

import com.simperium.client.Bucket;
import com.simperium.client.StorageProvider;
import android.util.Log;

import java.util.List;

public class NoteDB {

	private static final int DATABASE_VERSION = 1;
	private SQLiteDatabase db;
	private static final String DATABASE_NAME = "simplenote";

	private static final String CREATE_TABLE_NOTES = "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT, simperiumKey TEXT, title TEXT, content TEXT, contentPreview TEXT, creationDate DATE, modificationDate DATE, deleted BOOLEAN, lastPosition INTEGER, pinned BOOLEAN, shareURL TEXT, systemTags TEXT, tags TEXT);";
	private static final String ADD_NOTES_INDEX = "CREATE INDEX simperiumKeyNotesIndex ON notes(simperiumKey);";

	private static final String CREATE_TABLE_TAGS = "CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT, tagIndex INTEGER, simperiumKey TEXT, name TEXT);";
	private static final String ADD_TAGS_INDEX = "CREATE INDEX simperiumKeyTagsIndex ON tags(simperiumKey);";

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
	private static final String TAG="Simplenote";
	private class SimperiumStore implements StorageProvider {
		/**
		 * Store bucket object data
		 */
		public void addObject(Bucket bucket, String key, Bucket.Syncable object){
			if (object instanceof Note) {
				create((Note) object);
			}
		}
		public void updateObject(Bucket bucket, String key, Bucket.Syncable object){
			if(object instanceof Note){
				update((Note) object);
			}
		}
		public void removeObject(Bucket bucket, String key){
			Log.d(TAG, String.format("Time to remove %s in %s", key, bucket.getName()));
		}
		/**
		 * Retrieve entities and details
		 */
		public Map<String,Object> getObject(Bucket<?> bucket, String key) {
			
			String[] args = { key };
			Cursor c;
			if (bucket.getName().equals(Note.BUCKET_NAME)) {
				c = db.query(NOTES_TABLE, new String[] { "rowid _id", "simperiumKey", "title", "content", "contentPreview",
						"creationDate", "modificationDate", "deleted", "lastPosition", "pinned", "shareURL", "systemTags", "tags" },
						"simperiumKey=?", args, null, null, null);
				int count = c.getCount();
				c.moveToFirst();
				if (count > 0) {
					Map<String, Object> noteMap = new HashMap<String, Object>();
					noteMap.put("simperiumKey", c.getString(1));
					noteMap.put("title", c.getString(2));
					noteMap.put("content", c.getString(3));
					noteMap.put("contentPreview", c.getString(4));
					noteMap.put("creationDate", c.getLong(5));
					noteMap.put("modificationDate", c.getLong(6));
					noteMap.put("deleted", c.getInt(7));
					noteMap.put("lastPosition", c.getInt(8));
					noteMap.put("pinned", c.getInt(9));
					noteMap.put("shareURL", c.getString(10));
					noteMap.put("systemTags", c.getString(11));
					noteMap.put("tags", c.getString(12));
					c.close();
					return noteMap;
				}
			} else if (bucket.getName().equals(Tag.BUCKET_NAME)) {
				c = db.query(TAGS_TABLE, new String[] { "rowid _id", "simperiumKey", "tagIndex" },
						"simperiumKey=?", args, null, null, null);
				int count = c.getCount();
				c.moveToFirst();
				if (count > 0) {
					Map<String, Object> tagMap = new HashMap<String, Object>();
					tagMap.put("simperiumKey", c.getString(1));
					tagMap.put("tagIndex", c.getString(2));
					c.close();
					return tagMap;
				}
			}
			return null;
		}
		public <T extends Bucket.Syncable> List<T> allEntities(Bucket<T> bucket) {
			return null;
		}
		
	}
	
}
