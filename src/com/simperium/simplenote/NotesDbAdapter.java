package com.simperium.simplenote;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.util.Log;



/** Eric says: this code is straight from a tutorial. I just changed some stuff.
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class NotesDbAdapter 
{

    public static final String KEY_NOTE = "note";
    public static final String KEY_ROWID = "row_id";
    public static final String KEY_CREATION_DATE = "creation_date";
    public static final String KEY_MODIFICATION_DATE = "modification_date";
    private static final String TAG = "SimplenoteDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table notes (row_id integer primary key autoincrement, "+ 
                    KEY_NOTE +" text not null,"+
                    KEY_CREATION_DATE + " text not null,"+
                    KEY_MODIFICATION_DATE + " text not null);";

    private static final String DATABASE_NAME = "simplenote_data";
    private static final String DATABASE_TABLE = "notes";
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper 
    {

        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public NotesDbAdapter(Context ctx) 
    {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public NotesDbAdapter open() throws SQLException
    {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
   //    mDbHelper.onUpgrade(mDb, DATABASE_VERSION, DATABASE_VERSION);
        return this;
    }
    
    public void close() 
    {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createNote(String note, String creationDate, String modificationDate) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NOTE, note);
        initialValues.put(KEY_CREATION_DATE, creationDate);
        initialValues.put(KEY_MODIFICATION_DATE, modificationDate);
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteNote(long rowId) 
    {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllNotes() 
    {
    	try{
   
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NOTE, KEY_CREATION_DATE, KEY_MODIFICATION_DATE}, null, null, null, null, null);
    	}
    	catch(SQLiteException e)
    	{
    		return null;
    	}
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchNote(long rowId) throws SQLException 
    {
        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                        KEY_NOTE, KEY_CREATION_DATE, KEY_MODIFICATION_DATE}, KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) 
        {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateNote(long rowId, String note, String creationDate, String modificationDate) 
    {
        ContentValues args = new ContentValues();
        args.put(KEY_NOTE, note);
        args.put(KEY_CREATION_DATE, creationDate);
        args.put(KEY_MODIFICATION_DATE, modificationDate);
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
