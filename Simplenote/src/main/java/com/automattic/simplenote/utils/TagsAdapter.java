package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
public class TagsAdapter extends BaseAdapter {

    public static final String ID_COLUMN = "_id";
    public static final long ALL_NOTES_ID = -1L;
    public static final long TRASH_ID = -2L;

    public static final int DEFAULT_ITEM_POSITION = 0;

    protected Cursor mCursor;
    protected Context mContext;
    protected LayoutInflater mInflater;
    protected Bucket<Note> mNotesBucket;

    protected TagMenuItem mAllNotesItem;
    protected TagMenuItem mTrashItem;

    private int mNameColumn;
    private int mCountColumn;
    private int mRowIdColumn;
    private int mTextColorId;
    private int mHeaderCount;

    protected static final int[] topItems = { R.string.notes, R.string.trash };

    public TagsAdapter(Context context, Bucket<Note> notesBucket, int headerCount) {
        this(context, notesBucket, null);
        mHeaderCount = headerCount;
    }

    public TagsAdapter(Context context, Bucket<Note> notesBucket, Cursor cursor){
        mContext = context;
        mNotesBucket = notesBucket;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAllNotesItem = new TagMenuItem(ALL_NOTES_ID, R.string.notes){

            @Override
            public Query<Note> query(){
                return Note.all(mNotesBucket);
            }

        };
        mTrashItem = new TagMenuItem(TRASH_ID, R.string.trash){

            @Override
            public Query<Note> query(){
                return Note.allDeleted(mNotesBucket);
            }

        };

        TypedArray a = mContext.obtainStyledAttributes(new int[]{R.attr.noteTitleColor});
        mTextColorId = a.getResourceId(0, 0);
        a.recycle();

        swapCursor(cursor);
    }

    public Cursor swapCursor(Cursor cursor){
        Cursor oldCursor = mCursor;
        mCursor = cursor;
        if (mCursor != null){
            mNameColumn = cursor.getColumnIndexOrThrow(Tag.NAME_PROPERTY);
            mCountColumn = cursor.getColumnIndexOrThrow(Tag.NOTE_COUNT_INDEX_NAME);
            mRowIdColumn = cursor.getColumnIndexOrThrow(ID_COLUMN);
        }
        notifyDataSetChanged();
        return oldCursor;
    }

    public void changeCursor(Cursor cursor){
        Cursor oldCursor = swapCursor(cursor);
        if (oldCursor != null) oldCursor.close();
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
            return mAllNotesItem;
        } else if (i==1){
            return mTrashItem;
        } else {
            mCursor.moveToPosition(i-topItems.length);
            return new TagMenuItem(mCursor.getLong(mRowIdColumn),
                StrUtils.notNullStr(mCursor.getString(mNameColumn)));
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        if (view == null){
            view = mInflater.inflate(R.layout.tag_drawer_row, null);
        }
        TagMenuItem tagMenuItem = getItem(position);

        TextView labelText = (TextView) view.findViewById(R.id.tag_name);
        labelText.setText(tagMenuItem.name);

        int selectedPosition = ((ListView)viewGroup).getCheckedItemPosition() - mHeaderCount;
        if (position == selectedPosition)
            labelText.setTextColor(mContext.getResources().getColor(R.color.simplenote_blue));
        else
            labelText.setTextColor(mContext.getResources().getColor(mTextColorId));

        ImageView drawerIcon = (ImageView) view.findViewById(R.id.drawer_icon);
        View dividerView = view.findViewById(R.id.section_divider);
        drawerIcon.setColorFilter(mContext.getResources().getColor(mTextColorId));
        if (position == 0) {
            if (position == selectedPosition) {
                drawerIcon.setImageResource(R.drawable.ic_drawer_all_notes_selected);
                drawerIcon.setColorFilter(Color.argb(0, 0, 0, 0));
            } else {
                drawerIcon.setImageResource(R.drawable.ic_drawer_all_notes);
            }
            drawerIcon.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.GONE);
        } else if (position == 1) {
            if (position == selectedPosition) {
                drawerIcon.setImageResource(R.drawable.ic_drawer_trash_selected);
                drawerIcon.setColorFilter(Color.argb(0, 0, 0, 0));
            } else {
                drawerIcon.setImageResource(R.drawable.ic_drawer_trash);
            }
            drawerIcon.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.VISIBLE);
        } else {
            drawerIcon.setVisibility(View.GONE);
            dividerView.setVisibility(View.GONE);
        }

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
        public long id;

        private TagMenuItem(){
            name = "";
            id = -3L;
        }

        private TagMenuItem(long id, int resourceId){
            this(id, mContext.getResources().getString(resourceId));
        }

        private TagMenuItem(long id, String name){
            this.id = id;
            this.name = name;
        }

        public Query<Note> query(){
            return Note.allInTag(mNotesBucket, this.name);
        }
    }
}
