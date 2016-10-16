package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.TagsActivity;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.widgets.TintedTextView;
import com.simperium.client.Bucket;
import com.simperium.client.Query;

/**
 * Created by beaucollins on 7/26/13.
 */
public class TagsAdapter extends BaseAdapter {

    public static final String ID_COLUMN = "_id";
    public static final long ALL_NOTES_ID = -1L;
    public static final long TRASH_ID = -2L;
    public static final long TEMPLATES_ID = -3L;
    public static final long ALL_REMINDERS_ID = -4L;

    public static final int DEFAULT_ITEM_POSITION = 0;

    protected Cursor mCursor;
    protected Context mContext;
    protected LayoutInflater mInflater;
    protected Bucket<Note> mNotesBucket;

    protected TagMenuItem mAllNotesItem;
    protected TagMenuItem mTrashItem;
    protected TagMenuItem mTemplateItem;
    protected TagMenuItem mAllRemindersItem;

    private int mNameColumn;
    private int mRowIdColumn;
    private int mTextColorId;
    private int mHeaderCount;

    protected static final int[] topItems = { R.string.notes, R.string.trash, R.string.templates, R.string.reminders };

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

        mTemplateItem = new TagMenuItem(TEMPLATES_ID, R.string.templates){

            @Override
            public Query<Note> query(){
                return Note.allTemplates(mNotesBucket);
            }

        };

        mAllRemindersItem = new TagMenuItem(ALL_REMINDERS_ID, R.string.reminders){

            @Override
            public Query<Note> query(){
                return Note.allReminders(mNotesBucket);
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
        if (i == 0) {
            return mAllNotesItem;
        } else if (i == 1) {
            return mTrashItem;
        } else if (i == 2) {
            return mTemplateItem;
        } else if (i == 3){
            return mAllRemindersItem;
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
            view = mInflater.inflate(R.layout.nav_drawer_row, null);
        }TagMenuItem tagMenuItem = getItem(position);

        TintedTextView drawerItemText = (TintedTextView) view.findViewById(R.id.drawer_item_name);
        drawerItemText.setText(tagMenuItem.name);

        int selectedPosition = ((ListView)viewGroup).getCheckedItemPosition() - mHeaderCount;

        @ColorInt int color = ContextCompat.getColor(mContext, mTextColorId);
        if (position == selectedPosition) {
            color = ContextCompat.getColor(mContext, R.color.simplenote_blue);
        }

        View dividerView = view.findViewById(R.id.section_divider);

        Drawable icon = null;
        if (position == 0) {
            icon = ContextCompat.getDrawable(mContext, R.drawable.ic_notes_24dp);
            dividerView.setVisibility(View.GONE);
        } else if (position == 1) {
            icon = ContextCompat.getDrawable(mContext, R.drawable.ic_trash_24dp);
            dividerView.setVisibility(View.GONE);
        }  else if (position == 2) {
            icon = ContextCompat.getDrawable(mContext, R.drawable.ic_template_24dp);
            dividerView.setVisibility(View.GONE);
        } else if (position == 3) {
            icon = ContextCompat.getDrawable(mContext, R.drawable.ic_reminder_24dp);
            dividerView.setVisibility(View.VISIBLE);
        }
        else {
            dividerView.setVisibility(View.GONE);
        }

        drawerItemText.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null, color);
        drawerItemText.setTextColor(color);

        View tagsHeader = view.findViewById(R.id.tags_header);
        if (position == 4) {
            tagsHeader.setVisibility(View.VISIBLE);
        } else {
            tagsHeader.setVisibility(View.GONE);
        }

        View editTags = view.findViewById(R.id.edit_tags);
        editTags.setOnClickListener(mEditTagsOnClickListener);

        return view;
    }

    private View.OnClickListener mEditTagsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mContext != null) {
                Intent editTagsIntent = new Intent(mContext, TagsActivity.class);
                mContext.startActivity(editTagsIntent);
            }
        }
    };

    public int getPosition(TagMenuItem mSelectedTag) {
        if (mSelectedTag.id == ALL_NOTES_ID) return 0;
        if (mSelectedTag.id == TRASH_ID) return 1;
        if (mSelectedTag.id == TEMPLATES_ID) return 2;
        if (mSelectedTag.id == ALL_REMINDERS_ID) return 3;
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
            id = -5L;
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
