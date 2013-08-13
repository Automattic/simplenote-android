package com.automattic.simplenote.utils;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.simperium.client.Bucket;
import com.simperium.client.Query;

/**
 * Created by beaucollins on 7/26/13.
 */
public class TagSpinnerAdapter extends BaseAdapter {

    public static final String ID_COLUMN = "_id";
    public static final long ALL_NOTES_ID = -1L;
    public static final long TRASH_ID = -2L;

    public static final int DEFAULT_ITEM_POSITION = 0;

    protected Cursor mCursor;
    protected Context mContext;
    protected LayoutInflater mInflater;
    protected Bucket<Note> mNotesBucket;

    protected TagMenuItem mAllNotesCount;
    protected TagMenuItem mTrashCount;

    int mNameColumn;
    int mCountColumn;
    int mRowIdColumn;

    protected static final int[] topItems = { R.string.notes, R.string.trash };

    public TagSpinnerAdapter(Context context, Bucket<Note> notesBucket){
        this(context, notesBucket, null);
    }

    public TagSpinnerAdapter(Context context, Bucket<Note> notesBucket, Cursor cursor){
        mContext = context;
        mNotesBucket = notesBucket;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAllNotesCount = new TagMenuItem(ALL_NOTES_ID, R.string.notes, Note.all(mNotesBucket).count()){

            @Override
            public Query<Note> query(){
                return Note.all(mNotesBucket);
            }

        };
        mTrashCount = new TagMenuItem(TRASH_ID, R.string.trash, Note.allDeleted(mNotesBucket).count()){

            @Override
            public Query<Note> query(){
                return Note.allDeleted(mNotesBucket);
            }

        };

        swapCursor(cursor);
    }

    public void swapCursor(Cursor cursor){
        if (mCursor != null){
            mCursor.close();
        }
        mCursor = cursor;
        if (mCursor != null){
            mNameColumn = cursor.getColumnIndexOrThrow(Tag.NAME_PROPERTY);
            mCountColumn = cursor.getColumnIndexOrThrow(Tag.NOTE_COUNT_INDEX_NAME);
            mRowIdColumn = cursor.getColumnIndexOrThrow(ID_COLUMN);
            mAllNotesCount.setCount(Note.all(mNotesBucket).count());
            mTrashCount.setCount(Note.allDeleted(mNotesBucket).count());
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mCursor == null){
            return topItems.length;
        } else {
            return mCursor.getCount() + topItems.length;
        }
    }

    public TagMenuItem getDefaultItem(){
        return getItem(DEFAULT_ITEM_POSITION);
    }

    @Override
    public TagMenuItem getItem(int i) {
        if (i==0){
            return mAllNotesCount;
        } else if (i==1){
            return mTrashCount;
        } else {
            mCursor.moveToPosition(i-topItems.length);
            return new TagMenuItem(mCursor.getLong(mRowIdColumn),
                mCursor.getString(mNameColumn), mCursor.getInt(mCountColumn));
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mInflater.inflate(android.R.layout.simple_spinner_item, null);
        }

        TextView textView = (TextView) view;
        textView.setTypeface(Typefaces.get(mContext, Simplenote.CUSTOM_FONT_PATH));
        TagMenuItem menuItem = getItem(position);

        textView.setText(menuItem.name);
        return view;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent){
        if (view == null){
            view = mInflater.inflate(R.layout.tag_spinner_row, null);
        }
        TagMenuItem tagCount = getItem(position);

        TextView labelText = (TextView) view.findViewById(R.id.tag_name);
        labelText.setTypeface(Typefaces.get(mContext, Simplenote.CUSTOM_FONT_PATH));
        labelText.setText(tagCount.name);

        TextView countText = (TextView) view.findViewById(R.id.tag_count);
        countText.setText(tagCount.count);
        return view;
    }

    public int getPosition(TagMenuItem mSelectedTag) {
        if (mSelectedTag.id == ALL_NOTES_ID) return 0;
        if (mSelectedTag.id == TRASH_ID) return 1;
        if (mCursor == null) return -1;
        int current = mCursor.getPosition();
        mCursor.moveToPosition(-1);
        while(mCursor.moveToNext()){
            if (mSelectedTag.id == mCursor.getLong(mRowIdColumn)){
                int position = mCursor.getPosition();
                mCursor.moveToPosition(current);
                return position + topItems.length;
            }
        }
        mCursor.moveToPosition(current);
        return -1;
    }

    public class TagMenuItem {
        public String name;
        public String count;
        public long id;

        private TagMenuItem(){
            name = "";
            id = -3L;
            count = "";
        }

        private TagMenuItem(long id, int resourceId){
            this(id, mContext.getResources().getString(resourceId), 0);
        }

        private TagMenuItem(long id, int resourceId, int count){
            this(id, mContext.getResources().getString(resourceId), count);
        }

        private TagMenuItem(long id, String name, int count){
            this.id = id;
            this.name = name;
            setCount(count);
        }

        public Query<Note> query(){
            return Note.allInTag(mNotesBucket, this.name);
        }

        public void setCount(int count) {
            if (count > 0) {
                this.count = Integer.toString(count);
            } else {
                this.count = "";
            }
        }
    }
}
