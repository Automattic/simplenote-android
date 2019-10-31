package com.automattic.simplenote.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.automattic.android.tracks.datasets.SqlUtils;
import com.automattic.simplenote.utils.DateTimeUtils;

import java.util.Date;

public class SuggestionTable {
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_SEARCH = "search";
    public static final String TABLE_SUGGESTIONS = "tbl_search_suggestions";

    private static final String INDEX_QUERY = "idx_search_suggestions_query";

    protected static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SUGGESTIONS + " ("
            + " " + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " " + COLUMN_SEARCH + " TEXT NOT NULL COLLATE NOCASE,"
            + " " + COLUMN_DATE + " TEXT)"
        );
        db.execSQL("CREATE UNIQUE INDEX " + INDEX_QUERY + " ON " + TABLE_SUGGESTIONS + "(" + COLUMN_SEARCH + ")");
    }

    public static void deleteQueries() {
        SqlUtils.deleteAllRowsInTable(SuggestionDatabase.getDatabaseWritable(), TABLE_SUGGESTIONS);
    }

    public static void deleteQuery(@NonNull String query) {
        String[] args = new String[]{query};
        SuggestionDatabase.getDatabaseWritable().delete(TABLE_SUGGESTIONS, "" + COLUMN_SEARCH + "=?", args);
    }

    protected static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUGGESTIONS);
    }

    /**
     * Returns {@link Cursor} containing queries previously used by user.
     *
     * @param filter    filters list using LIKE syntax (null for no filter)
     * @param max       limit list to this many items (zero for no limit)
     */
    public static Cursor getQueries(String filter, int max) {
        String sql;
        String[] args;

        if (TextUtils.isEmpty(filter)) {
            sql = "SELECT * FROM " + TABLE_SUGGESTIONS;
            args = null;
        } else {
            sql = "SELECT * FROM " + TABLE_SUGGESTIONS + " WHERE " + COLUMN_SEARCH + " LIKE ?";
            args = new String[]{"%" + filter + "%"};
        }

        sql += " ORDER BY " + COLUMN_DATE + " DESC";

        if (max > 0) {
            sql += " LIMIT " + max;
        }

        return SuggestionDatabase.getDatabaseReadable().rawQuery(sql, args);
    }

    public static void insertOrReplaceQuery(@NonNull String query) {
        String date = DateTimeUtils.getISO8601FromDate(new Date());
        SQLiteStatement statement = SuggestionDatabase.getDatabaseWritable().compileStatement(
                "INSERT OR REPLACE INTO " + TABLE_SUGGESTIONS + " (" + COLUMN_SEARCH + ", " + COLUMN_DATE + ") VALUES (?1,?2)"
        );

        try {
            statement.bindString(1, query);
            statement.bindString(2, date);
            statement.execute();
        } finally {
            SqlUtils.closeStatement(statement);
        }
    }
}
