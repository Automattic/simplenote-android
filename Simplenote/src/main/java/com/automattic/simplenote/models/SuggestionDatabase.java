package com.automattic.simplenote.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.automattic.simplenote.Simplenote;

public class SuggestionDatabase extends SQLiteOpenHelper {
    private static SuggestionDatabase mDatabase;

    private static final String DATABASE_NAME = "suggestion.db";
    private static final Object DATABASE_LOCK = new Object();
    private static final String TAG = SuggestionDatabase.class.getSimpleName();
    /*
     * 1 - initial version
     */
    private static final int VERSION = 1;

    private SuggestionDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Do NOT call super() here; it throws SQLiteException.
        Log.i(TAG, "Downgrading database from version " + oldVersion + " to version " + newVersion);
        reset(database);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Reset database when upgrading; future versions may want to avoid this
        // and modify table structures on upgrade while preserving data.
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset(database);
    }

    public static SuggestionDatabase getDatabase() {
        if (mDatabase == null) {
            synchronized (DATABASE_LOCK) {
                if (mDatabase == null) {
                    mDatabase = new SuggestionDatabase(Simplenote.requireContext());
                    // This ensures onOpen() is called with a writable database
                    // (open will fail if getDatabaseReadable() is called first).
                    mDatabase.getWritableDatabase();
                }
            }
        }

        return mDatabase;
    }

    private void createTables(SQLiteDatabase database) {
        SuggestionTable.createTable(database);
    }

    private void dropTables(SQLiteDatabase database) {
        SuggestionTable.dropTable(database);
    }

    public static SQLiteDatabase getDatabaseReadable() {
        return getDatabase().getReadableDatabase();
    }

    public static SQLiteDatabase getDatabaseWritable() {
        return getDatabase().getWritableDatabase();
    }

    private void reset(SQLiteDatabase database) {
        database.beginTransaction();

        try {
            dropTables(database);
            createTables(database);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }
}
