package com.automattic.simplenote;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.BaseCursorAdapter;
import com.automattic.simplenote.widgets.EmptyViewRecyclerView;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.Query;

import java.lang.ref.SoftReference;
import java.util.List;

public class TagsListFragment extends Fragment implements ActionMode.Callback, Bucket.Listener<Tag> {

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
        return inflater.inflate(R.layout.fragment_tags_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        Simplenote application = (Simplenote) getActivity().getApplication();
        mTagsBucket = application.getTagsBucket();
        mNotesBucket = application.getNotesBucket();

        EmptyViewRecyclerView recyclerView = getActivity().findViewById(R.id.list);
        mTagsAdapter = new TagsAdapter();
        recyclerView.setAdapter(mTagsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        View emptyView = getActivity().findViewById(R.id.empty);
        ImageView emptyViewImage = emptyView.findViewById(R.id.image);
        emptyViewImage.setImageResource(R.drawable.ic_tag_24dp);
        TextView emptyViewText = emptyView.findViewById(R.id.text);
        emptyViewText.setText(R.string.empty_tags);
        recyclerView.setEmptyView(emptyView);

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

        mNotesBucket.stop();
        mTagsBucket.stop();
    }

    protected void refreshTags() {
        Query<Tag> tagQuery = Tag.all(mTagsBucket).reorder().orderByKey().include(Tag.NOTE_COUNT_INDEX_NAME);
        Bucket.ObjectCursor<Tag> cursor = tagQuery.execute();
        mTagsAdapter.swapCursor(cursor);
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
        if (menuItem.getItemId() == R.id.menu_delete) {
            actionMode.finish(); // Action picked, so close the CAB
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mActionMode = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            if (isAdded()) {
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

    private static class removeTagFromNotesTask extends AsyncTask<Tag, Void, Void> {

        private SoftReference<TagsListFragment> fragmentRef;

        private removeTagFromNotesTask(TagsListFragment context) {
            fragmentRef = new SoftReference<>(context);
        }

        @Override
        protected Void doInBackground(Tag... removedTags) {
            TagsListFragment fragment = fragmentRef.get();
            Tag tag = removedTags[0];
            if (tag != null) {
                Bucket.ObjectCursor<Note> notesCursor = tag.findNotes(fragment.mNotesBucket);
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

    private class TagsAdapter extends BaseCursorAdapter<TagsAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private TextView tagTitle;
            private TextView tagCountTextView;
            private ImageButton deleteButton;

            private ViewHolder(View itemView) {
                super(itemView);

                tagTitle = itemView.findViewById(R.id.tag_name);
                tagCountTextView = itemView.findViewById(R.id.tag_count);
                deleteButton = itemView.findViewById(R.id.tag_trash);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!isAdded() || !hasItem(getAdapterPosition())) {
                            return;
                        }

                        final Tag tag = ((Bucket.ObjectCursor<Tag>) getItem(getAdapterPosition())).getObject();
                        final int tagCount = mNotesBucket.query().where("tags", Query.ComparisonType.EQUAL_TO, tag.getName()).count();
                        if (tagCount == 0) {
                            deleteTag(tag);
                        } else if (tagCount > 0) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.Dialog));
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
                deleteButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (v.isHapticFeedbackEnabled()) {
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        }

                        Toast.makeText(getContext(), getContext().getString(R.string.delete_tag), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                if (!isAdded() || !hasItem(getAdapterPosition())) {
                    return;
                }

                final AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.Dialog));
                LinearLayout alertView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.edit_tag, null);

                final Tag tag = ((Bucket.ObjectCursor<Tag>) getItem(getAdapterPosition())).getObject();

                final EditText tagNameEditText = alertView.findViewById(R.id.tag_name_edit);
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

            private void deleteTag(Tag tag) {
                tag.delete();
                new removeTagFromNotesTask(TagsListFragment.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tag);
                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.TAG_MENU_DELETED,
                        AnalyticsTracker.CATEGORY_TAG,
                        "list_trash_button"
                );
            }
        }

        private TagsAdapter() {
            super(null);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            View contactView = inflater.inflate(R.layout.tags_list_row, parent, false);

            return new ViewHolder(contactView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, Cursor cursor) {
            Tag tag = ((Bucket.ObjectCursor<Tag>)cursor).getObject();

            holder.tagTitle.setText(tag.getName());
            final int tagCount = mNotesBucket.query().where("tags", Query.ComparisonType.EQUAL_TO, tag.getName()).count();
            if (tagCount > 0) {
                holder.tagCountTextView.setText(String.valueOf(tagCount));
            } else {
                holder.tagCountTextView.setText("");
            }
        }

        @Override
        public void swapCursor(Cursor newCursor) {
            super.swapCursor(newCursor);
        }
    }
}
