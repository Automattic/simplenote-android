package com.automattic.simplenote;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.TypefaceSpan;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.Query;

import java.util.List;

/**
 * Created by Dan Roundhill on 6/26/13. (In Greece!)
 */
public class TagsListActivity extends ListActivity implements AdapterView.OnItemClickListener, ActionMode.Callback, AbsListView.MultiChoiceModeListener, AdapterView.OnItemLongClickListener, Bucket.Listener<Tag> {

    private ActionMode mActionMode;
    private Bucket<Tag> mTagsBucket;
    private Bucket<Note> mNotesBucket;
    private TagsAdapter mTagsAdapter;

    private Tracker mTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        ThemeUtils.setTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tags_list);

        SpannableString s = new SpannableString(getString(R.string.edit_tags));
        s.setSpan(new TypefaceSpan(this), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActionBar().setTitle(s);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        TextView emptyTextView = (TextView)findViewById(android.R.id.empty);
        emptyTextView.setText(Html.fromHtml("<strong>" + getString(R.string.no_tags_found) + "</strong>"));

        ListView listView = getListView();
        listView.setMultiChoiceModeListener(this);
        listView.setOnItemClickListener(this);
        // Disabling long press CAB action for now since bulk deleting is incomplete
        // listView.setOnItemLongClickListener(this);

        Simplenote application = (Simplenote) getApplication();
        mTagsBucket = application.getTagsBucket();
        mNotesBucket = application.getNotesBucket();

        mTagsAdapter = new TagsAdapter(getBaseContext(), null, 0);
        setListAdapter(mTagsAdapter);
        refreshTags();

        EasyTracker.getInstance().activityStart(this);
        mTracker = EasyTracker.getTracker();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNotesBucket.start();
        mTagsBucket.start();

        mTagsBucket.addOnNetworkChangeListener(this);
        mTagsBucket.addOnSaveObjectListener(this);
        mTagsBucket.addOnDeleteObjectListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mTagsBucket.removeOnNetworkChangeListener(this);
        mTagsBucket.removeOnSaveObjectListener(this);
        mTagsBucket.removeOnDeleteObjectListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    protected void refreshTags() {
        Query<Tag> tagQuery = Tag.all(mTagsBucket).reorder().orderByKey().include(Tag.NOTE_COUNT_INDEX_NAME);
        ObjectCursor<Tag> cursor = tagQuery.execute();
        mTagsAdapter.changeCursor(cursor);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int row, long l) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LinearLayout alertView = (LinearLayout) getLayoutInflater().inflate(R.layout.edit_tag, null);

        final Tag tag = mTagsAdapter.getItem(row);

        final EditText tagNameEditText = (EditText) alertView.findViewById(R.id.tag_name_edit);
        tagNameEditText.setText(tag.getName());
        tagNameEditText.setSelection(tagNameEditText.length());
        alert.setView(alertView);
        alert.setTitle(R.string.rename_tag);
        alert.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = tagNameEditText.getText().toString().trim();
                try {
                    tag.renameTo(value, mNotesBucket);
                    mTracker.sendEvent("tag", "edited_tag", "tag_alert_edit_box", null);
                } catch (BucketObjectNameInvalid e) {
                    android.util.Log.e(Simplenote.TAG, "Unable to rename tag", e);
                    // TODO: show user a message that new tag name is not ok
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing
            }
        });
        alert.show();

    }

    // TODO: Finish bulk editing
    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.bulk_edit_tags, menu);
        mActionMode = actionMode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_delete:
                actionMode.finish(); // Action picked, so close the CAB
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mActionMode = null;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
        final int checkedCount = getListView().getCheckedItemCount();
        switch (checkedCount) {
            case 0:
                actionMode.setSubtitle(null);
                break;
            case 1:
                actionMode.setSubtitle("One item selected");
                break;
            default:
                actionMode.setSubtitle("" + checkedCount + " items selected");
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setItemChecked(position, true);
        if (mActionMode == null)
            startActionMode(this);
        //isCABDestroyed = false;
        return false; // so this action does not consume the event!!!
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Tag Bucket listeners
    @Override
    public void onDeleteObject(Bucket<Tag> bucket, Tag object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshTags();
            }
        });
    }

    @Override
    public void onChange(Bucket<Tag> bucket, Bucket.ChangeType type, String key) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshTags();
            }
        });
    }

    @Override
    public void onSaveObject(Bucket<Tag> bucket, Tag object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshTags();
            }
        });
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Tag> bucket, Tag object) {
        // noop
    }

    private class TagsAdapter extends CursorAdapter {

        private ObjectCursor<Tag> mCursor;

        public TagsAdapter(Context context, ObjectCursor<Tag> c, int flags) {
            super(context, c, flags);
            mCursor = c;
        }

        public void changeCursor(ObjectCursor<Tag> cursor) {
            super.changeCursor(cursor);
            mCursor = cursor;
        }

        @Override
        public Tag getItem(int row) {
            super.getItem(row);
            return mCursor.getObject();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            mCursor.moveToPosition(position);

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.tags_list_row, null);
            }
            final Tag tag = mCursor.getObject();
            convertView.setTag(tag.getSimperiumKey());

            TextView tagTitle = (TextView) convertView.findViewById(R.id.tag_name);
            TextView tagCountTextView = (TextView) convertView.findViewById(R.id.tag_count);
            tagTitle.setText(tag.getName());
            final int tagCount = mNotesBucket.query().where("tags", Query.ComparisonType.EQUAL_TO, tag.getSimperiumKey()).count();
            if (tagCount > 0) {
                tagCountTextView.setText(Integer.toString(tagCount));
            } else {
                tagCountTextView.setText("");
            }

            ImageButton deleteButton = (ImageButton) convertView.findViewById(R.id.tag_trash);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (tagCount == 0) {
                        deleteTag(tag);
                    } else if (tagCount > 0) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(TagsListActivity.this);
                        alert.setTitle(R.string.delete_tag);
                        alert.setMessage(getString(R.string.confirm_delete_tag));
                        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteTag(tag);
                            }
                        });
                        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing, just closing the dialog
                            }
                        });
                        alert.show();
                    }
                }
            });

            return convertView;
        }

        private void deleteTag(Tag tag) {
            tag.delete();
            new removeTagFromNotesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tag);
            mTracker.sendEvent("tag", "deleted_tag", "list_trash_button", null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

        }
    }

    private class removeTagFromNotesTask extends AsyncTask<Tag, Void, Void> {

        @Override
        protected Void doInBackground(Tag... removedTags) {
            Tag tag = removedTags[0];
            if (tag != null) {
                ObjectCursor<Note> notesCursor = tag.findNotes(mNotesBucket);
                while (notesCursor.moveToNext()) {
                    Note note = notesCursor.getObject();
                    List<String> tags = note.getTags();
                    tags.remove(tag.getName());
                    note.setTags(tags);
                    note.save();
                }
                notesCursor.close();
            }
            return null;
        }
    }
}
