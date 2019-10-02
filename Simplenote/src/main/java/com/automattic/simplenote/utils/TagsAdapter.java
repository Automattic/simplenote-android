package com.automattic.simplenote.utils;

import android.content.Context;
import android.database.Cursor;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.automattic.simplenote.R;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.simperium.client.Bucket;
import com.simperium.client.Query;

public class TagsAdapter extends BaseAdapter {
    public static final int DEFAULT_ITEM_POSITION = 0;
    public static final int ALL_NOTES_ID = -1;
    public static final int TRASH_ID = -2;
    public static final int SETTINGS_ID = -3;
    public static final int TAGS_ID = -4;
    public static final int UNTAGGED_NOTES_ID = -5;

    private static final String ID_COLUMN = "_id";
    private static final int mMinimumItemsPrimary = new int[] {R.string.all_notes, R.string.trash}.length;
    private static final int mMinimumItemsSecondary = new int[] {R.string.untagged_notes}.length;

    private Bucket<Note> mNotesBucket;
    private Context mContext;
    private Cursor mCursor;
    private TagMenuItem mAllNotesItem;
    private TagMenuItem mTrashItem;
    private TagMenuItem mUntaggedNotesItem;
    private int mNameColumn;
    private int mRowIdColumn;

    public TagsAdapter(Context context, Bucket<Note> notesBucket) {
        this(context, notesBucket, null);
    }

    private TagsAdapter(Context context, Bucket<Note> notesBucket, Cursor cursor) {
        mContext = context;
        mNotesBucket = notesBucket;
        mAllNotesItem = new TagMenuItem(ALL_NOTES_ID, R.string.all_notes) {
            @Override
            public Query<Note> query() {
                return Note.all(mNotesBucket);
            }
        };
        mTrashItem = new TagMenuItem(TRASH_ID, R.string.trash) {
            @Override
            public Query<Note> query() {
                return Note.allDeleted(mNotesBucket);
            }
        };
        mUntaggedNotesItem = new TagMenuItem(UNTAGGED_NOTES_ID, R.string.untagged_notes) {
            @Override
            public Query<Note> query() {
                return Note.allWithNoTag(mNotesBucket);
            }
        };

        swapCursor(cursor);
    }

    private Cursor swapCursor(Cursor cursor) {
        Cursor oldCursor = mCursor;
        mCursor = cursor;
        if (mCursor != null) {
            mNameColumn = cursor.getColumnIndexOrThrow(Tag.NAME_PROPERTY);
            mRowIdColumn = cursor.getColumnIndexOrThrow(ID_COLUMN);
        }
        notifyDataSetChanged();
        return oldCursor;
    }

    public void changeCursor(Cursor cursor) {
        Cursor oldCursor = swapCursor(cursor);
        if (oldCursor != null) oldCursor.close();
    }

    @Override
    public int getCount() {
        if (mCursor == null) {
            return mMinimumItemsPrimary + mMinimumItemsSecondary;
        } else {
            return mMinimumItemsPrimary + mMinimumItemsSecondary + mCursor.getCount();
        }
    }

    public int getCountCustom() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public TagMenuItem getDefaultItem() {
        return getItem(DEFAULT_ITEM_POSITION);
    }

    @Override
    public TagMenuItem getItem(int i) {
        if (i == 0) {
            return mAllNotesItem;
        } else if (i == 1) {
            return mTrashItem;
        } else if (i == this.getCount() - 1) {
            return mUntaggedNotesItem;
        } else {
            mCursor.moveToPosition(i - mMinimumItemsPrimary);
            return new TagMenuItem(mCursor.getLong(mRowIdColumn),
                    StrUtils.notNullStr(mCursor.getString(mNameColumn)));
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    public int getPosition(TagMenuItem mSelectedTag) {
        if (mSelectedTag.id == ALL_NOTES_ID) return 0;
        if (mSelectedTag.id == TRASH_ID) return 1;
        if (mSelectedTag.id == UNTAGGED_NOTES_ID) return this.getCount() - 1;
        if (mCursor == null) return -1;
        int current = mCursor.getPosition();
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            if (mSelectedTag.id == mCursor.getLong(mRowIdColumn)) {
                int position = mCursor.getPosition();
                mCursor.moveToPosition(current);
                return position + mMinimumItemsPrimary;
            }
        }
        mCursor.moveToPosition(current);
        return -1;
    }

    public TagMenuItem getTagFromItem(MenuItem item) {
        switch (item.getItemId()) {
            case ALL_NOTES_ID:
                return mAllNotesItem;
            case TRASH_ID:
                return mTrashItem;
            case UNTAGGED_NOTES_ID:
                return mUntaggedNotesItem;
            default:
                return new TagMenuItem(
                    item.getItemId(),
                    item.getTitle().toString()
                );
        }
    }

    public class TagMenuItem {
        public String name;
        public long id;

        private TagMenuItem(long id, String name) {
            this.id = id;
            this.name = name;
        }

        private TagMenuItem(long id, int resourceId) {
            this(id, mContext.getResources().getString(resourceId));
        }

        public Query<Note> query() {
            return Note.allInTag(mNotesBucket, this.name);
        }
    }
}
