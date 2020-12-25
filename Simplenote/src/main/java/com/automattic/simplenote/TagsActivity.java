package com.automattic.simplenote;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.BaseCursorAdapter;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.HtmlCompat;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.EmptyViewRecyclerView;
import com.simperium.client.Bucket;
import com.simperium.client.Query;

import java.lang.ref.SoftReference;
import java.util.List;

import static com.automattic.simplenote.TagDialogFragment.DIALOG_TAG;
import static com.automattic.simplenote.models.Note.TAGS_PROPERTY;
import static com.automattic.simplenote.models.Tag.NAME_PROPERTY;
import static com.automattic.simplenote.utils.DisplayUtils.disableScreenshotsIfLocked;

public class TagsActivity extends ThemedAppCompatActivity implements Bucket.Listener<Tag> {
    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;
    private EmptyViewRecyclerView mTagsList;
    private ImageView mEmptyViewImage;
    private MenuItem mSearchMenuItem;
    private String mSearchQuery;
    private TagsAdapter mTagsAdapter;
    private TextView mEmptyViewText;
    private boolean mIsSearching;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tags);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SpannableString title = new SpannableString(getString(R.string.edit_tags));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Simplenote application = (Simplenote) getApplication();
        mTagsBucket = application.getTagsBucket();
        mNotesBucket = application.getNotesBucket();

        mTagsList = findViewById(R.id.list);
        mTagsAdapter = new TagsAdapter();
        mTagsList.setAdapter(mTagsAdapter);
        mTagsList.setLayoutManager(new LinearLayoutManager(TagsActivity.this));
        View emptyView = findViewById(R.id.empty);
        mEmptyViewImage = emptyView.findViewById(R.id.image);
        mEmptyViewText = emptyView.findViewById(R.id.text);
        checkEmptyList();
        mTagsList.setEmptyView(emptyView);

        refreshTags();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tags_list, menu);
        DrawableUtils.tintMenuWithAttribute(TagsActivity.this, menu, R.attr.toolbarIconColor);

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchMenuItem.setOnActionExpandListener(
            new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    mIsSearching = false;
                    return true;
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    mIsSearching = true;
                    return true;
                }
            }
        );
        SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
        LinearLayout searchEditFrame = searchView.findViewById(R.id.search_edit_frame);
        ((LinearLayout.LayoutParams) searchEditFrame.getLayoutParams()).leftMargin = 0;

        // Workaround for setting the search placeholder text color.
        @SuppressWarnings("ResourceType")
        String hintHexColor = getString(R.color.text_title_disabled).replace("ff", "");
        searchView.setQueryHint(
            HtmlCompat.fromHtml(
                String.format(
                    "<font color=\"%s\">%s</font>",
                    hintHexColor,
                    getString(R.string.search_tags_hint)
                )
            )
        );

        searchView.setOnQueryTextListener(
            new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String query) {
                    if (mSearchMenuItem.isActionViewExpanded()) {
                        mSearchQuery = query;
                        refreshTagsSearch();
                        mTagsList.scrollToPosition(0);
                        checkEmptyList();
                    }

                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String queryText) {
                    return true;
                }
            }
        );

        searchView.setOnCloseListener(
            new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    mIsSearching = false;
                    mSearchQuery = "";
                    refreshTags();
                    mTagsList.scrollToPosition(0);
                    checkEmptyList();
                    return false;
                }
            }
        );

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableScreenshotsIfLocked(this);

        mTagsBucket.addOnNetworkChangeListener(this);
        mTagsBucket.addOnSaveObjectListener(this);
        mTagsBucket.addOnDeleteObjectListener(this);
        AppLog.add(AppLog.Type.SYNC, "Added tag bucket listener (TagsActivity)");
    }

    @Override
    public void onPause() {
        super.onPause();

        mTagsBucket.removeOnNetworkChangeListener(this);
        mTagsBucket.removeOnSaveObjectListener(this);
        mTagsBucket.removeOnDeleteObjectListener(this);
        AppLog.add(AppLog.Type.SYNC, "Removed tag bucket listener (TagsActivity)");
    }

    public void checkEmptyList() {
        if (mIsSearching) {
            if (DisplayUtils.isLandscape(TagsActivity.this) && !DisplayUtils.isLargeScreen(TagsActivity.this)) {
                setEmptyListImage(-1);
            } else {
                setEmptyListImage(R.drawable.ic_search_24dp);
            }

            setEmptyListMessage(getString(R.string.empty_tags_search));
        } else {
            setEmptyListImage(R.drawable.ic_tag_24dp);
            setEmptyListMessage(getString(R.string.empty_tags));
        }
    }

    protected void refreshTags() {
        Query<Tag> tagQuery = Tag.all(mTagsBucket).reorder().orderByKey().include(Tag.NOTE_COUNT_INDEX_NAME);
        Bucket.ObjectCursor<Tag> cursor = tagQuery.execute();
        mTagsAdapter.swapCursor(cursor);
    }

    protected void refreshTagsSearch() {
        Query<Tag> tags = Tag.all(mTagsBucket)
            .where(NAME_PROPERTY, Query.ComparisonType.LIKE, "%" + mSearchQuery + "%")
            .orderByKey().include(Tag.NOTE_COUNT_INDEX_NAME)
            .reorder();
        Bucket.ObjectCursor<Tag> cursor = tags.execute();
        mTagsAdapter.swapCursor(cursor);
    }

    private void setEmptyListImage(@DrawableRes int image) {
        if (mEmptyViewImage != null) {
            if (image != -1) {
                mEmptyViewImage.setVisibility(View.VISIBLE);
                mEmptyViewImage.setImageResource(image);
            } else {
                mEmptyViewImage.setVisibility(View.GONE);
            }
        }
    }

    private void setEmptyListMessage(String message) {
        if (mEmptyViewText != null && message != null) {
            mEmptyViewText.setText(message);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Tag> bucket, Tag object) {
    }

    @Override
    public void onDeleteObject(Bucket<Tag> bucket, Tag object) {
        runOnUiThread(
            new Runnable() {
                @Override
                public void run() {
                    if (mIsSearching) {
                        refreshTagsSearch();
                    } else {
                        refreshTags();
                    }
                }
            }
        );
    }

    @Override
    public void onNetworkChange(Bucket<Tag> bucket, Bucket.ChangeType type, String key) {
        runOnUiThread(
            new Runnable() {
                @Override
                public void run() {
                    if (mIsSearching) {
                        refreshTagsSearch();
                    } else {
                        refreshTags();
                    }
                }
            }
        );
    }

    @Override
    public void onSaveObject(Bucket<Tag> bucket, Tag object) {
        runOnUiThread(
            new Runnable() {
                @Override
                public void run() {
                    if (mIsSearching) {
                        refreshTagsSearch();
                    } else {
                        refreshTags();
                    }
                }
            }
        );
    }

    private static class RemoveTagFromNotesTask extends AsyncTask<Tag, Void, Void> {
        private SoftReference<TagsActivity> mTagsActivityReference;

        private RemoveTagFromNotesTask(TagsActivity context) {
            mTagsActivityReference = new SoftReference<>(context);
        }

        @Override
        protected Void doInBackground(Tag... removedTags) {
            TagsActivity activity = mTagsActivityReference.get();
            Tag tag = removedTags[0];

            if (tag != null) {
                Bucket.ObjectCursor<Note> cursor = tag.findNotes(activity.mNotesBucket, tag.getName());

                while (cursor.moveToNext()) {
                    Note note = cursor.getObject();
                    List<String> tags = note.getTags();
                    tags.remove(tag.getName());
                    note.setTags(tags);
                    note.save();
                }

                cursor.close();
            }

            return null;
        }
    }

    private class TagsAdapter extends BaseCursorAdapter<TagsAdapter.ViewHolder> {
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private TextView mCount;
            private TextView mTitle;

            private ViewHolder(View itemView) {
                super(itemView);

                mTitle = itemView.findViewById(R.id.tag_name);
                mCount = itemView.findViewById(R.id.tag_count);

                ImageButton deleteButton = itemView.findViewById(R.id.tag_trash);
                deleteButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!hasItem(getAdapterPosition())) {
                                return;
                            }

                            @SuppressWarnings("unchecked")
                            final Tag tag = ((Bucket.ObjectCursor<Tag>) getItem(getAdapterPosition())).getObject();
                            final int tagCount = mNotesBucket.query().where(TAGS_PROPERTY, Query.ComparisonType.EQUAL_TO, tag.getName()).count();

                            if (tagCount == 0) {
                                deleteTag(tag);
                            } else if (tagCount > 0) {
                                AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(TagsActivity.this, R.style.Dialog));
                                alert.setTitle(R.string.delete_tag);
                                alert.setMessage(getString(R.string.confirm_delete_tag));
                                alert.setNegativeButton(R.string.no, null);
                                alert.setPositiveButton(
                                    R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            deleteTag(tag);
                                        }
                                    }
                                );
                                alert.show();
                            }
                        }
                    }
                );
                deleteButton.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (v.isHapticFeedbackEnabled()) {
                                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            }

                            Toast.makeText(TagsActivity.this, getString(R.string.delete_tag), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
                );

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                if (!hasItem(getAdapterPosition())) {
                    return;
                }

                //noinspection unchecked
                TagDialogFragment dialog = new TagDialogFragment(
                    ((Bucket.ObjectCursor<Tag>) getItem(getAdapterPosition())).getObject(),
                    mNotesBucket,
                    mTagsBucket
                );
                dialog.show(getSupportFragmentManager().beginTransaction(), DIALOG_TAG);
            }

            private void deleteTag(Tag tag) {
                tag.delete();
                new RemoveTagFromNotesTask(TagsActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tag);
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
        public TagsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View contactView = inflater.inflate(R.layout.tags_list_row, parent, false);
            return new TagsAdapter.ViewHolder(contactView);
        }

        @Override
        public void onBindViewHolder(@NonNull TagsAdapter.ViewHolder holder, Cursor cursor) {
            @SuppressWarnings("unchecked")
            Tag tag = ((Bucket.ObjectCursor<Tag>)cursor).getObject();
            holder.mTitle.setText(tag.getName());
            final int tagCount = mNotesBucket.query().where(TAGS_PROPERTY, Query.ComparisonType.EQUAL_TO, tag.getName()).count();

            if (tagCount > 0) {
                holder.mCount.setText(String.valueOf(tagCount));
            } else {
                holder.mCount.setText("");
            }
        }

        @Override
        public void swapCursor(Cursor newCursor) {
            super.swapCursor(newCursor);
        }
    }
}
