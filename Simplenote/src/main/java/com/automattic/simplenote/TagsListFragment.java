package com.automattic.simplenote;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.ActionMode;
import android.view.LayoutInflater;
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

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.Query;

import java.util.List;

public class TagsListFragment extends ListFragment implements AdapterView.OnItemClickListener, ActionMode.Callback, AbsListView.MultiChoiceModeListener, AdapterView.OnItemLongClickListener, Bucket.Listener<Tag>{

    private ActionMode mActionMode;
    private Bucket<Tag> mTagsBucket;
    private Bucket<Note> mNotesBucket;
    private TagsAdapter mTagsAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TagsListFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tags_list, container, false);

        TextView emptyTextView = (TextView)view.findViewById(android.R.id.empty);
        emptyTextView.setText(Html.fromHtml("<strong>" + getString(R.string.no_tags_found) + "</strong>"));
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView listView = getListView();
        listView.setMultiChoiceModeListener(this);
        listView.setOnItemClickListener(this);
        // Disabling long press CAB action for now since bulk deleting is incomplete
        // listView.setOnItemLongClickListener(this);
        setHasOptionsMenu(true);

        Simplenote application = (Simplenote) getActivity().getApplication();
        mTagsBucket = application.getTagsBucket();
        mNotesBucket = application.getNotesBucket();

        mTagsAdapter = new TagsAdapter(getActivity().getBaseContext(), null, 0);
        setListAdapter(mTagsAdapter);
        refreshTags();
    }

    @Override
    public void onResume() {
        super.onResume();

        mNotesBucket.start();
        mTagsBucket.start();

        mTagsBucket.addOnNetworkChangeListener(this);
        mTagsBucket.addOnSaveObjectListener(this);
        mTagsBucket.addOnDeleteObjectListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        mTagsBucket.removeOnNetworkChangeListener(this);
        mTagsBucket.removeOnSaveObjectListener(this);
        mTagsBucket.removeOnDeleteObjectListener(this);
    }

    protected void refreshTags() {
        Query<Tag> tagQuery = Tag.all(mTagsBucket).reorder().orderByKey().include(Tag.NOTE_COUNT_INDEX_NAME);
        Bucket.ObjectCursor<Tag> cursor = tagQuery.execute();
        mTagsAdapter.changeCursor(cursor);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int row, long l) {
        if (!isAdded()) return;

        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        LinearLayout alertView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.edit_tag, null);

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
                    AnalyticsTracker.track(
                            AnalyticsTracker.Stat.TAG_EDITOR_ACCESSED,
                            AnalyticsTracker.CATEGORY_TAG,
                            "tag_alert_edit_box"
                    );
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
        inflater.inflate(R.menu.bulk_edit, menu);
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
        if (isAdded() && mActionMode == null) {
            getActivity().startActionMode(this);
        }
        //isCABDestroyed = false;
        return false; // so this action does not consume the event!!!
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            if (isAdded()){
                getActivity().finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // Tag Bucket listeners
    @Override
    public void onDeleteObject(Bucket<Tag> bucket, Tag object) {
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshTags();
                }
            });
        }
    }

    @Override
    public void onNetworkChange(Bucket<Tag> bucket, Bucket.ChangeType type, String key) {
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshTags();
                }
            });
        }
    }

    @Override
    public void onSaveObject(Bucket<Tag> bucket, Tag object) {
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshTags();
                }
            });
        }
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Tag> bucket, Tag object) {
        // noop
    }

    private class TagsAdapter extends CursorAdapter {

        private Bucket.ObjectCursor<Tag> mCursor;

        public TagsAdapter(Context context, Bucket.ObjectCursor<Tag> c, int flags) {
            super(context, c, flags);
            mCursor = c;
        }

        public void changeCursor(Bucket.ObjectCursor<Tag> cursor) {
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
                convertView = getActivity().getLayoutInflater().inflate(R.layout.tags_list_row, null);
            }
            final Tag tag = mCursor.getObject();
            convertView.setTag(tag.getSimperiumKey());

            TextView tagTitle = (TextView) convertView.findViewById(R.id.tag_name);
            TextView tagCountTextView = (TextView) convertView.findViewById(R.id.tag_count);
            tagTitle.setText(tag.getName());
            final int tagCount = mNotesBucket.query().where("tags", Query.ComparisonType.EQUAL_TO, tag.getSimperiumKey()).count();
            if (tagCount > 0) {
                tagCountTextView.setText(String.valueOf(tagCount));
            } else {
                tagCountTextView.setText("");
            }

            ImageButton deleteButton = (ImageButton) convertView.findViewById(R.id.tag_trash);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!isAdded()) return;

                    if (tagCount == 0) {
                        deleteTag(tag);
                    } else if (tagCount > 0) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
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
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.TAG_MENU_DELETED,
                    AnalyticsTracker.CATEGORY_TAG,
                    "list_trash_button"
            );
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
                Bucket.ObjectCursor<Note> notesCursor = tag.findNotes(mNotesBucket);
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
