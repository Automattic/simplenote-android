package com.automattic.simplenote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.util.Log;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.client.Bucket;
import com.simperium.client.Syncable;
import com.simperium.storage.StorageProvider;

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

	private static final String[] NOTES_FIELDS = new String[] { "rowid _id", "simperiumKey", "title", "content", "contentPreview",
						"creationDate", "modificationDate", "deleted", "lastPosition", "pinned", "shareURL", "systemTags", "tags" };
	private static final String[] TAGS_FIELDS = new String[] { "rowid _id", "simperiumKey", "tagIndex", "name" };

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
		values.put("creationDate", note.getCreationDate().getTimeInMillis() / 1000);
		values.put("modificationDate", note.getModificationDate().getTimeInMillis() / 1000);
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
		values.put("tagIndex", tag.getTagIndex());
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
		values.put("creationDate", note.getCreationDate().getTimeInMillis() / 1000);
		values.put("modificationDate", note.getModificationDate().getTimeInMillis() / 1000);
		values.put("deleted", note.isDeleted());
		values.put("lastPosition", note.getLastPosition());
		values.put("pinned", note.isPinned());
		values.put("shareURL", note.getShareURL());
		values.put("systemTags", new JSONArray(note.getSystemTags()).toString());
		values.put("tags", new JSONArray(note.getTags()).toString());

		return db.update(NOTES_TABLE, values, "simperiumKey=?", new String[] { note.getSimperiumKey() }) > 0;
	}
	
	// nbradbury - returns true if passed note exists
	boolean exists(Note note) {
		if (note==null)
			return false;
		try {
			long value = DatabaseUtils.longForQuery(db, "SELECT 1 FROM " + NOTES_TABLE + " WHERE simperiumKey=?", new String[]{note.getSimperiumKey()});
			return (value==1);
		} catch (SQLiteDoneException e) {
			return false;
		}
	}

	boolean update(Tag tag) {
		if (tag == null)
			return false;

		ContentValues values = new ContentValues();
		values.put("simperiumKey", tag.getSimperiumKey());
		values.put("index", tag.getTagIndex());
		values.put("name", tag.getName());

		return db.update(TAGS_TABLE, values, "simperiumKey=?", new String[] { tag.getSimperiumKey() }) > 0;
	}

	// nbradbury - returns true if passed tag exists
	boolean exists(Tag tag) {
		if (tag==null)
			return false;
		try {
			long value = DatabaseUtils.longForQuery(db, "SELECT 1 FROM " + TAGS_TABLE + " WHERE simperiumKey=?", new String[]{tag.getSimperiumKey()});
			return (value==1);
		} catch (SQLiteDoneException e) {
			return false;
		}
	}
	
	boolean delete(Note note) {
		if (note == null)
			return false;

		return db.delete(NOTES_TABLE, "simperiumKey=?", new String[] { note.getSimperiumKey() }) > 0;
	}

	boolean delete(Tag tag) {
		if (tag == null)
			return false;

		return db.delete(TAGS_TABLE, "simperiumKey=?", new String[] { tag.getSimperiumKey() }) > 0;
	}

	public Cursor fetchAllNotes(Context context) {
		return fetchNotes(context, false, null);
	}

	public Cursor fetchDeletedNotes(Context context) {
		return fetchNotes(context, true, null);
	}

	public Cursor fetchNotesByTag(Context context, String tagName) {
		return fetchNotes(context, false, tagName);
	}

	public Cursor fetchNotes(Context context, boolean deleted, String tagName) {

		// get sort preference, default to sorting by modified date
		int sortPref = PrefUtils.getIntPref(context, PrefUtils.PREF_SORT_ORDER);
		String orderBy = "modificationDate DESC";

		String whereClause = "deleted = ?";
		String[] whereArgs = new String[] { deleted ? "1" : "0" };
		if (tagName == null) {
			// nbradbury - changed content sorting to use COLLATE NOCASE for case-insensitive sorting
			switch (sortPref) {
			case 1:
				orderBy = "creationDate DESC";
				break;
			case 2:
				orderBy = "content COLLATE NOCASE ASC";
				break;
			case 3:
				orderBy = "modificationDate ASC";
				break;
			case 4:
				orderBy = "creationDate ASC";
				break;
			case 5:
				orderBy = "content COLLATE NOCASE DESC";
				break;
			}
		} else {
			whereClause += " AND tags LIKE ?";
			whereArgs = new String[] { deleted ? "1" : "0", "%\"" + tagName + "\"%"};
		}

		Cursor cursor = db.query(NOTES_TABLE, new String[] { "rowid _id", "simperiumKey", "title", "content", "contentPreview",
				"creationDate", "modificationDate", "deleted", "lastPosition", "pinned", "shareURL", "systemTags", "tags" }, whereClause,
				whereArgs, null, null, "pinned DESC, " + orderBy);

		Log.d(Simplenote.TAG, String.format("Found %d notes", cursor.getCount()));

		return cursor;
	}

	public String[] fetchAllTags() {
		String[] tags = null;
		Cursor c = db.query(TAGS_TABLE, new String[] { "rowid _id", "simperiumKey", "name", "tagIndex" }, null, null, null, null,
				"simperiumKey ASC");
		int numRows = c.getCount();
		tags = new String[numRows];
		c.moveToFirst();

		for (int i = 0; i < numRows; ++i) {
			tags[i] = c.getString(2);
			c.moveToNext();
		}
		c.close();
		Log.d(Simplenote.TAG, String.format("Found %d tags", c.getCount()));

		return tags;
	}

    public Cursor fetchAllTagsCursor() {
        String[] tags = null;
        Cursor c = db.query(TAGS_TABLE, new String[] { "rowid _id", "simperiumKey", "name", "tagIndex" }, null, null, null, null,
                "simperiumKey ASC");
        Log.d(Simplenote.TAG, String.format("Found %d tags", c.getCount()));

        return c;
    }

	public Cursor searchNotes(String searchString) {
		Cursor cursor = db.query(NOTES_TABLE, new String[] { "rowid _id", "simperiumKey", "title", "content", "contentPreview",
				"creationDate", "modificationDate", "deleted", "lastPosition", "pinned", "shareURL", "systemTags", "tags" },
				"content like ? AND deleted = ?", new String[]{"%" + searchString + "%", "0"}, null, null, "PINNED DESC");
		// if (cursor != null) {
		// cursor.moveToFirst();
		// }

		return cursor;
	}
	/**
	 * Given a cursor from a notes query retrive the properties as a Map
	 */
	public Map<String,Object> notePropertiesFromCursor(Cursor c){

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

		// Convert JSON strings to ArrayList instances
		try {
			JSONArray systemTagArray = new JSONArray(c.getString(11));
			JSONArray tagArray = new JSONArray(c.getString(12));

			ArrayList<String> systemTagList = new ArrayList<String>();
			for (int i=0; i<systemTagArray.length(); i++) {
			    systemTagList.add( systemTagArray.getString(i) );
			}

			ArrayList<String> tagList = new ArrayList<String>();
			for (int i=0; i<tagArray.length(); i++) {
			    tagList.add( tagArray.getString(i) );
			}
			noteMap.put("systemTags", systemTagList);
			noteMap.put("tags", tagList);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return noteMap;
	}
	/**
	 * Given a cursor from tags query retrive the properties as a Map
	 */
	public Map<String,Object> tagPropertiesFromCursor(Cursor c) {
		Map<String, Object> tagMap = new HashMap<String, Object>();
		tagMap.put("simperiumKey", c.getString(1));
		tagMap.put("tagIndex", c.getString(2));
		tagMap.put("name", c.getString(3));
		return tagMap;
	}

	public SimperiumStore getSimperiumStore() {
		return new SimperiumStore();
	}

	private class SimperiumStore implements StorageProvider {
		/**
		 * Store bucket object data
		 */
		public void addObject(Bucket bucket, String key, Syncable object) {
			if (object instanceof Note) {
				Log.d(Simplenote.TAG, String.format("Adding note %s", object));
				create((Note) object);
			} else if (object instanceof Tag) {
				Log.d(Simplenote.TAG, String.format("Adding tag %s", object));
				create((Tag) object);
			}
		}

		public void updateObject(Bucket bucket, String key, Syncable object) {
			if (object instanceof Note) {
				Note note = (Note)object;
				// nbradbury - create note if it doesn't exist - added after noticing that addNote() in NoteListFragment was triggering this method (updateObject) even though
				// it should've triggered addObject() since it's a new note - TODO: determine why this is happening so we can avoid exists() every time updateObject is called
				if (!exists(note)) {
					Log.d(Simplenote.TAG, String.format("Adding rather than updating note %s", object));
					create(note);
				} else {
					Log.d(Simplenote.TAG, String.format("Updating note %s", object));
					update(note);
				}
			} else if (object instanceof Tag) {
				// nbradbury - create tag if it doesn't exist
				Tag tag = (Tag)object;
				if (!exists(tag)) {
					Log.d(Simplenote.TAG, String.format("Adding rather than updating tag %s", object));
				} else {
					Log.d(Simplenote.TAG, String.format("Updating tag %s", object));
					update(tag);
				}
			}
		}

		public void removeObject(Bucket bucket, String key) {
			Log.d(Simplenote.TAG, String.format("Time to remove %s in %s", key, bucket.getName()));
		}

		/**
		 * Retrieve entities and details
		 */
		public Map<String, Object> getObject(Bucket<?> bucket, String key) {

			String[] args = { key };
			Cursor c;
			if (bucket.getName().equals(Note.BUCKET_NAME)) {
				c = db.query(NOTES_TABLE, NOTES_FIELDS,
						"simperiumKey=?", args, null, null, null);
				int count = c.getCount();
				c.moveToFirst();
				if (count > 0) {
					Map<String, Object> noteMap = notePropertiesFromCursor(c);
					c.close();
					return noteMap;
				}
				c.close();
			} else if (bucket.getName().equals(Tag.BUCKET_NAME)) {
				c = db.query(TAGS_TABLE, TAGS_FIELDS,
						"simperiumKey=?", args, null, null, null);
				int count = c.getCount();
				c.moveToFirst();
				if (count > 0) {
					Map<String, Object> tagMap = tagPropertiesFromCursor(c);
					c.close();
					return tagMap;
				}
				c.close();
			}
			return null;
		}
		/**
		 * Retrieve properties for every object in the bucket
		 */
		public List<Map<String,Object>> allObjects(Bucket<?> bucket) {
			Cursor c;
			if (bucket.getName().equals(Note.BUCKET_NAME)) {
				// get all notes
				c = db.query(NOTES_TABLE, NOTES_FIELDS, null, null, null, null, null);
				List<Map<String,Object>> notesData = new ArrayList<Map<String,Object>>();
				if (c.getCount() > 0) {
					c.moveToFirst();
					do {
						notesData.add(notePropertiesFromCursor(c));
					} while(c.moveToNext());
				}
				c.close();
				return notesData;
			} else if (bucket.getName().equals(Tag.BUCKET_NAME)) {
				c = db.query(TAGS_TABLE, TAGS_FIELDS, null, null, null, null, null);
				List<Map<String,Object>> tagsData = new ArrayList<Map<String,Object>>();
				if (c.getCount() > 0) {
					c.moveToFirst();
					do {
						tagsData.add(tagPropertiesFromCursor(c));
					} while(c.moveToNext());
				}
				c.close();
				return tagsData;
			}
			return null;
		}

        public List<String> allKeys(Bucket<?> bucket){
            return new ArrayList<String>();
        }

        public void resetBucket(Bucket<?> bucket){
            
        }
    }

}