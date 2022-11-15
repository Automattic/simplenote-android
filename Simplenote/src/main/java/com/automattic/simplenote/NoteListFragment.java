package com.automattic.simplenote;

import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_SEARCH;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.RECENT_SEARCH_TAPPED;
import static com.automattic.simplenote.models.Note.TAGS_PROPERTY;
import static com.automattic.simplenote.models.Preferences.MAX_RECENT_SEARCHES;
import static com.automattic.simplenote.models.Preferences.PREFERENCES_OBJECT_KEY;
import static com.automattic.simplenote.models.Suggestion.Type.HISTORY;
import static com.automattic.simplenote.models.Suggestion.Type.QUERY;
import static com.automattic.simplenote.models.Suggestion.Type.TAG;
import static com.automattic.simplenote.models.Tag.NAME_PROPERTY;
import static com.automattic.simplenote.utils.PrefUtils.ALPHABETICAL_ASCENDING;
import static com.automattic.simplenote.utils.PrefUtils.ALPHABETICAL_DESCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_CREATED_ASCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_CREATED_DESCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_MODIFIED_ASCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_MODIFIED_DESCENDING;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.ListFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.models.Suggestion;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.AppLog.Type;
import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.ChecklistUtils;
import com.automattic.simplenote.utils.DateTimeUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.NetworkUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.SearchSnippetFormatter;
import com.automattic.simplenote.utils.SearchTokenizer;
import com.automattic.simplenote.utils.SimplenoteLinkify;
import com.automattic.simplenote.utils.StrUtils;
import com.automattic.simplenote.utils.TextHighlighter;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.utils.WidgetUtils;
import com.automattic.simplenote.widgets.RobotoRegularTextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.Query;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A list fragment representing a list of Notes. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link NoteEditorFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends ListFragment implements AdapterView.OnItemLongClickListener, AbsListView.MultiChoiceModeListener, Bucket.Listener<Preferences> {
    public static final String TAG_PREFIX = "tag:";

    /**
     * The preferences key representing the activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final int POPUP_MENU_FIRST_ITEM_POSITION = 0;
    public static final String ACTION_NEW_NOTE = "com.automattic.simplenote.NEW_NOTE";
    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sCallbacks = new Callbacks() {
        @Override
        public void onActionModeCreated() {
        }

        @Override
        public void onActionModeDestroyed() {
        }

        @Override
        public void onNoteSelected(String noteID, String matchOffsets, boolean isMarkdownEnabled, boolean isPreviewEnabled) {
        }
    };
    protected NotesCursorAdapter mNotesAdapter;
    protected String mSearchString;
    private Bucket<Preferences> mBucketPreferences;
    private Bucket<Tag> mBucketTag;
    private ActionMode mActionMode;
    private View mRootView;
    private RobotoRegularTextView mEmptyViewButton;
    private ImageView mEmptyViewImage;
    private TextView mEmptyViewText;
    private View mDividerLine;
    private FloatingActionButton mFloatingActionButton;
    private boolean mIsCondensedNoteList;
    private boolean mIsSearching;
    private ListView mList;
    private RecyclerView mSuggestionList;
    private RelativeLayout mSuggestionLayout;
    private String mSelectedNoteId;
    private SuggestionAdapter mSuggestionAdapter;
    private RefreshListTask mRefreshListTask;
    private RefreshListForSearchTask mRefreshListForSearchTask;
    private int mDeletedItemIndex;
    private int mTitleFontSize;
    private int mPreviewFontSize;
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sCallbacks;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteListFragment() {
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setItemChecked(position, true);

        if (mActionMode == null) {
            requireActivity().startActionMode(this);
        }

        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        mCallbacks.onActionModeCreated();
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.bulk_edit, menu);
        DrawableUtils.tintMenuWithAttribute(getActivity(), menu, R.attr.actionModeTextColor);
        mActionMode = actionMode;
        requireActivity().getWindow().setStatusBarColor(ThemeUtils.getColorFromAttribute(requireContext(), R.attr.mainBackgroundColor));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (getListView().getCheckedItemIds().length > 0) {
            switch (item.getItemId()) {
                case R.id.menu_link:
                    AnalyticsTracker.track(
                        AnalyticsTracker.Stat.INTERNOTE_LINK_COPIED,
                        AnalyticsTracker.CATEGORY_LINK,
                        "internote_link_copied_list"
                    );
                    BrowserUtils.copyToClipboard(requireContext(), getSelectedNoteLinks());
                    mode.finish();
                    break;
                case R.id.menu_trash:
                    new TrashNotesTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case R.id.menu_pin:
                    new PinNotesTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
            }
        }

        return false;
    }

    private String getSelectedNoteLinks() {
        SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();
        StringBuilder links = new StringBuilder();

        for (int i = 0; i < checkedPositions.size(); i++) {
            if (checkedPositions.valueAt(i)) {
                Note note = mNotesAdapter.getItem(checkedPositions.keyAt(i));
                links.append(SimplenoteLinkify.getNoteLinkWithTitle(note.getTitle(), note.getSimperiumKey())).append("\n");
            }
        }

        return links.toString();
    }

    public List<Integer> getSelectedNotesPositions() {
        SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();
        ArrayList<Integer> positions = new ArrayList<>();

        for (int i = 0; i < checkedPositions.size(); i++) {
            if (checkedPositions.valueAt(i)) {
                positions.add(checkedPositions.keyAt(i) - mList.getHeaderViewsCount());
            }
        }

        return positions;
    }

    public void updateSelectionAfterTrashAction() {
        if (DisplayUtils.isLargeScreenLandscape(getActivity())) {
            // Try to find the nearest note to the first deleted item
            List<Integer> deletedNotesPositions = getSelectedNotesPositions();
            int firstDeletedNote = deletedNotesPositions.get(0);
            int positionToSelect = -1;
            // Loop through the notes below
            for (int i = firstDeletedNote + 1; i < mNotesAdapter.getCount(); i++) {
                if (!deletedNotesPositions.contains(i)) {
                    positionToSelect = i;
                    break;
                }
            }
            if (positionToSelect == -1) {
                // Loop through the above notes
                for (int i = firstDeletedNote - 1; i >= 0; i--) {
                    if (!deletedNotesPositions.contains(i)) {
                        positionToSelect = i;
                        break;
                    }
                }
            }

            if (positionToSelect != -1) {
                Note noteToSelect = mNotesAdapter.getItem(positionToSelect + mList.getHeaderViewsCount());
                mCallbacks.onNoteSelected(noteToSelect.getSimperiumKey(), null, noteToSelect.isMarkdownEnabled(), noteToSelect.isPreviewEnabled());
                // As we will trigger a list refresh later, save the selectedNoteId
                mSelectedNoteId = noteToSelect.getSimperiumKey();
            } else {
                // The list of notes is empty
                ((NotesActivity) requireActivity()).showDetailPlaceholder();
            }
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mCallbacks.onActionModeDestroyed();
        mActionMode = null;
        if (getActivity() != null) {
            NotesActivity notesActivity = (NotesActivity) getActivity();
            setActivateOnItemClick(DisplayUtils.isLargeScreenLandscape(notesActivity));
            if (mSelectedNoteId == null) {
                notesActivity.showDetailPlaceholder();
            }
        }
        new Handler().postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    requireActivity().getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent, requireActivity().getTheme()));
                }
            },
            requireContext().getResources().getInteger(android.R.integer.config_longAnimTime)
        );
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
        int checkedCount = getListView().getCheckedItemCount();

        if (checkedCount == 0) {
            actionMode.setTitle("");
        } else {
            actionMode.setTitle(getResources().getQuantityString(R.plurals.selected_notes, checkedCount, checkedCount));
        }

        actionMode.invalidate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.add(Type.NETWORK, NetworkUtils.getNetworkInfo(requireContext()));
        AppLog.add(Type.SCREEN, "Created (NoteListFragment)");
        mBucketPreferences = ((Simplenote) requireActivity().getApplication()).getPreferencesBucket();
        mBucketTag = ((Simplenote) requireActivity().getApplication()).getTagsBucket();
    }

    protected void getPrefs() {
        mIsCondensedNoteList = PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_CONDENSED_LIST, false);
        mTitleFontSize = PrefUtils.getFontSize(getActivity());
        mPreviewFontSize = mTitleFontSize - 2;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NotesActivity notesActivity = (NotesActivity) requireActivity();

        if (ACTION_NEW_NOTE.equals(notesActivity.getIntent().getAction()) &&
            !notesActivity.userIsUnauthorized()) {
            //if user tap on "app shortcut", create a new note
            createNewNote("", "new_note_shortcut");
        }

        mRootView = view.findViewById(R.id.list_root);

        LinearLayout emptyView = view.findViewById(android.R.id.empty);
        emptyView.setVisibility(View.GONE);
        mEmptyViewButton = emptyView.findViewById(R.id.button);
        mEmptyViewImage = emptyView.findViewById(R.id.image);
        mEmptyViewText = emptyView.findViewById(R.id.text);
        setEmptyListImage(R.drawable.ic_notes_24dp);
        setEmptyListMessage(getString(R.string.empty_notes_all));
        mDividerLine = view.findViewById(R.id.divider_line);

        if (DisplayUtils.isLargeScreenLandscape(notesActivity)) {
            setActivateOnItemClick(true);
            mDividerLine.setVisibility(View.VISIBLE);
        }

        mFloatingActionButton = view.findViewById(R.id.fab_button);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewNote("", "action_bar_button");
            }
        });
        mFloatingActionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v.isHapticFeedbackEnabled()) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(getContext(), requireContext().getString(R.string.new_note), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mSuggestionLayout = view.findViewById(R.id.suggestion_layout);
        mSuggestionList = view.findViewById(R.id.suggestion_list);
        mSuggestionAdapter = new SuggestionAdapter(new ArrayList<Suggestion>());
        mSuggestionList.setAdapter(mSuggestionAdapter);
        mSuggestionList.setLayoutManager(new LinearLayoutManager(requireContext()));

        mList = view.findViewById(android.R.id.list);

        mNotesAdapter = new NotesCursorAdapter(requireActivity().getBaseContext(), null, 0);
        setListAdapter(mNotesAdapter);

        getListView().setOnItemLongClickListener(this);
        getListView().setMultiChoiceModeListener(this);
    }

    public void showListPadding(boolean show) {
        mList.setPadding(
            mList.getPaddingLeft(),
            mList.getPaddingTop(),
            mList.getPaddingRight(),
            show ? (int) getResources().getDimension(R.dimen.note_list_item_padding_bottom_button) : 0
        );
    }

    public void createNewNote(String title, String label) {
        if (!isAdded()) {
            return;
        }

        addNote(title);
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.LIST_NOTE_CREATED,
            AnalyticsTracker.CATEGORY_NOTE,
            label
        );
    }

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPrefs();

        if (mIsSearching) {
            refreshListForSearch();
        } else {
            refreshList();
        }

        mBucketPreferences.start();
        mBucketPreferences.addOnDeleteObjectListener(this);
        mBucketPreferences.addOnNetworkChangeListener(this);
        mBucketPreferences.addOnSaveObjectListener(this);
        AppLog.add(Type.SYNC, "Added preference bucket listener (NoteListFragment)");
    }

    @Override
    public void onPause() {
        super.onPause();
        mBucketPreferences.removeOnDeleteObjectListener(this);
        mBucketPreferences.removeOnNetworkChangeListener(this);
        mBucketPreferences.removeOnSaveObjectListener(this);
        AppLog.add(Type.SYNC, "Removed preference bucket listener (NoteListFragment)");
        AppLog.add(Type.SCREEN, "Paused (NoteListFragment)");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sCallbacks;
    }

    public void setEmptyListButton(String message) {
        if (mEmptyViewButton != null) {
            if (!message.isEmpty()) {
                mEmptyViewButton.setVisibility(View.VISIBLE);
                mEmptyViewButton.setText(message);
            } else {
                mEmptyViewButton.setVisibility(View.GONE);
            }
        }
    }

    public void setEmptyListImage(@DrawableRes int image) {
        if (mEmptyViewImage != null) {
            if (image != -1) {
                mEmptyViewImage.setVisibility(View.VISIBLE);
                mEmptyViewImage.setImageResource(image);
            } else {
                mEmptyViewImage.setVisibility(View.GONE);
            }
        }
    }

    public void setEmptyListMessage(String message) {
        if (mEmptyViewText != null && message != null) {
            mEmptyViewText.setText(message);
        }
    }

    @Override
    public void onListItemClick(@NonNull ListView listView, @NonNull View view, int position, long id) {
        if (!isAdded()) return;
        super.onListItemClick(listView, view, position, id);

        NoteViewHolder holder = (NoteViewHolder) view.getTag();
        String noteID = holder.getNoteId();

        if (noteID != null) {
            Note note = mNotesAdapter.getItem(position);
            mCallbacks.onNoteSelected(noteID, holder.mMatchOffsets, note.isMarkdownEnabled(), note.isPreviewEnabled());
        }

        mActivatedPosition = position;
    }

    /**
     * Selects first row in the list if available
     */
    public void selectFirstNote() {
        if (mNotesAdapter.getCount() > 0) {
            Note selectedNote = mNotesAdapter.getItem(mList.getHeaderViewsCount());
            mCallbacks.onNoteSelected(selectedNote.getSimperiumKey(), null, selectedNote.isMarkdownEnabled(), selectedNote.isPreviewEnabled());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public View getRootView() {
        return mRootView;
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
    }

    public void setActivatedPosition(int position) {
        if (getListView() != null) {
            if (position == ListView.INVALID_POSITION) {
                getListView().setItemChecked(mActivatedPosition, false);
            } else {
                getListView().setItemChecked(position, true);
            }

            mActivatedPosition = position;
        }
    }

    public void setDividerVisible(boolean visible) {
        mDividerLine.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setFloatingActionButtonVisible(boolean visible) {
        if (mFloatingActionButton == null) return;

        if (visible) {
            mFloatingActionButton.show();
        } else {
            mFloatingActionButton.hide();
        }
    }

    public void refreshList() {
        refreshList(false);
    }

    public void refreshList(boolean fromNav) {
        if (mRefreshListTask != null && mRefreshListTask.getStatus() != AsyncTask.Status.FINISHED) {
            mRefreshListTask.cancel(true);
        }

        mRefreshListTask = new RefreshListTask(this);
        mRefreshListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fromNav);

        WidgetUtils.updateNoteWidgets(requireActivity().getApplicationContext());
    }

    private void refreshListForSearch() {
        if (mRefreshListForSearchTask != null && mRefreshListForSearchTask.getStatus() != AsyncTask.Status.FINISHED) {
            mRefreshListForSearchTask.cancel(true);
        }

        mRefreshListForSearchTask = new RefreshListForSearchTask(this);
        mRefreshListForSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void refreshListFromNavSelect() {
        refreshList(true);
    }

    public ObjectCursor<Note> queryNotes() {
        if (!isAdded()) return null;

        NotesActivity notesActivity = (NotesActivity) requireActivity();
        Query<Note> query = notesActivity.getSelectedTag().query();

        String searchString = mSearchString;
        if (hasSearchQuery()) {
            searchString = queryTags(query, mSearchString);
        }
        if (!TextUtils.isEmpty(searchString)) {
            query.where(new Query.FullTextMatch(new SearchTokenizer(searchString)));
            query.include(new Query.FullTextOffsets("match_offsets"));
            query.include(new Query.FullTextSnippet(Note.MATCHED_TITLE_INDEX_NAME, Note.TITLE_INDEX_NAME));
            query.include(new Query.FullTextSnippet(Note.MATCHED_CONTENT_INDEX_NAME, Note.CONTENT_PROPERTY));
            query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        } else {
            query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        }

        query.include(Note.PINNED_INDEX_NAME);
        PrefUtils.sortNoteQuery(query, requireContext(), true);
        return query.execute();
    }

    private ObjectCursor<Note> queryNotesForSearch() {
        if (!isAdded()) {
            return null;
        }

        Query<Note> query = Note.all(((Simplenote) requireActivity().getApplication()).getNotesBucket());
        String searchString = mSearchString;

        if (hasSearchQuery()) {
            searchString = queryTags(query, mSearchString);
        }

        if (!TextUtils.isEmpty(searchString)) {
            query.where(new Query.FullTextMatch(new SearchTokenizer(searchString)));
            query.include(new Query.FullTextOffsets("match_offsets"));
            query.include(new Query.FullTextSnippet(Note.MATCHED_TITLE_INDEX_NAME, Note.TITLE_INDEX_NAME));
            query.include(new Query.FullTextSnippet(Note.MATCHED_CONTENT_INDEX_NAME, Note.CONTENT_PROPERTY));
            query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        } else {
            query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        }

        PrefUtils.sortNoteQuery(query, requireContext(), false);
        return query.execute();
    }

    private String queryTags(Query<Note> query, String searchString) {
        Pattern pattern = Pattern.compile(TAG_PREFIX + "(.*?)( |$)");
        Matcher matcher = pattern.matcher(searchString);
        while (matcher.find()) {
            query.where(TAGS_PROPERTY, Query.ComparisonType.LIKE, matcher.group(1));
        }
        return matcher.replaceAll("");
    }

    public void addNote(String title) {
        // Prevents jarring 'New note...' from showing in the list view when creating a new note
        NotesActivity notesActivity = (NotesActivity) requireActivity();

        if (!DisplayUtils.isLargeScreenLandscape(notesActivity)) {
            notesActivity.stopListeningToNotesBucket();
        }

        // Create & save new note
        Simplenote simplenote = (Simplenote) requireActivity().getApplication();
        Bucket<Note> notesBucket = simplenote.getNotesBucket();
        final Note note = notesBucket.newObject();
        note.setContent(title);
        note.setCreationDate(Calendar.getInstance());
        note.setModificationDate(note.getCreationDate());
        note.setMarkdownEnabled(PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_MARKDOWN_ENABLED, false));

        if (notesActivity.getSelectedTag() != null && notesActivity.getSelectedTag().name != null) {
            String tagName = notesActivity.getSelectedTag().name;

            if (!tagName.equals(getString(R.string.all_notes)) && !tagName.equals(getString(R.string.trash)) && !tagName.equals(getString(R.string.untagged_notes))) {
                note.setTagString(tagName);
            }
        }

        note.save();

        if (DisplayUtils.isLargeScreenLandscape(getActivity())) {
            // Hack: Simperium saves async so we add a small delay to ensure the new note is truly
            // saved before proceeding.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCallbacks.onNoteSelected(note.getSimperiumKey(), null, note.isMarkdownEnabled(), note.isPreviewEnabled());
                }
            }, 50);
        } else {
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, true);
            arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());
            arguments.putBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED, note.isPreviewEnabled());
            Intent editNoteIntent = new Intent(getActivity(), NoteEditorActivity.class);
            editNoteIntent.putExtras(arguments);

            requireActivity().startActivityForResult(editNoteIntent, Simplenote.INTENT_EDIT_NOTE);
        }
    }

    public void setNoteSelected(String selectedNoteID) {
        // Loop through notes and set note selected if found
        //noinspection unchecked
        ObjectCursor<Note> cursor = (ObjectCursor<Note>) mNotesAdapter.getCursor();
        if (cursor != null) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String noteKey = cursor.getSimperiumKey();
                if (noteKey != null && noteKey.equals(selectedNoteID)) {
                    setActivatedPosition(i + mList.getHeaderViewsCount());
                    return;
                }
            }
        }

        // Didn't find the note, let's try again after the cursor updates (see RefreshListTask)
        mSelectedNoteId = selectedNoteID;
    }

    public void searchNotes(String searchString, boolean isSubmit) {
        mIsSearching = true;
        mSuggestionLayout.setVisibility(View.VISIBLE);

        if (!searchString.equals(mSearchString)) {
            mSearchString = searchString;
        }

        if (searchString.isEmpty()) {
            getSearchItems();
        } else {
            getTagSuggestions(searchString);
        }

        if (isSubmit) {
            mSuggestionLayout.setVisibility(View.GONE);
            refreshListForSearch();
        }
    }

    /**
     * Clear search and load all notes
     */
    public void clearSearch() {
        mIsSearching = false;
        mSuggestionLayout.setVisibility(View.GONE);
        refreshList();

        if (mSearchString != null && !mSearchString.equals("")) {
            mSearchString = null;
            refreshList();
        }
    }

    public boolean hasSearchQuery() {
        return mSearchString != null && !mSearchString.equals("");
    }

    public void addSearchItem(String item, int index) {
        Preferences preferences = getPreferences();

        if (preferences != null) {
            List<String> recents = preferences.getRecentSearches();
            recents.remove(item);
            recents.add(index, item);
            // Trim recent searches to MAX_RECENT_SEARCHES (currently 5) if size is greater than MAX_RECENT_SEARCHES.
            preferences.setRecentSearches(recents.subList(0, recents.size() > MAX_RECENT_SEARCHES ? MAX_RECENT_SEARCHES : recents.size()));
            preferences.save();
        } else {
            Log.e("addSearchItem", "Could not get preferences entity");
        }
    }

    private void deleteSearchItem(String item) {
        Preferences preferences = getPreferences();

        if (preferences != null) {
            List<String> recents = preferences.getRecentSearches();
            mDeletedItemIndex = recents.indexOf(item);
            recents.remove(item);
            preferences.setRecentSearches(recents);
            preferences.save();
        } else {
            Log.e("deleteSearchItem", "Could not get preferences entity");
        }
    }

    private Preferences getPreferences() {
        try {
            return mBucketPreferences.get(PREFERENCES_OBJECT_KEY);
        } catch (BucketObjectMissingException exception) {
            try {
                Preferences preferences = mBucketPreferences.newObject(PREFERENCES_OBJECT_KEY);
                preferences.save();
                return preferences;
            } catch (BucketObjectNameInvalid invalid) {
                Log.e("getPreferences", "Could not create preferences entity", invalid);
                return null;
            }
        }
    }

    private void getSearchItems() {
        Preferences preferences = getPreferences();

        if (preferences != null) {
            ArrayList<Suggestion> suggestions = new ArrayList<>();

            for (String recent : preferences.getRecentSearches()) {
                suggestions.add(new Suggestion(recent, HISTORY));
            }

            mSuggestionAdapter.updateItems(suggestions);
        } else {
            Log.e("getSearchItems", "Could not get preferences entity");
        }
    }

    private void getTagSuggestions(String query) {
        ArrayList<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion(query, QUERY));
        Query<Tag> tags = Tag.all(mBucketTag).reorder().order(Tag.NOTE_COUNT_INDEX_NAME, Query.SortType.DESCENDING);

        if (!query.endsWith(TAG_PREFIX)) {
            tags.where(NAME_PROPERTY, Query.ComparisonType.LIKE, "%" + query + "%");
        }

        try (ObjectCursor<Tag> cursor = tags.execute()) {
            while (cursor.moveToNext()) {
                suggestions.add(new Suggestion(cursor.getObject().getName(), TAG));
            }
        }

        mSuggestionAdapter = new SuggestionAdapter(suggestions);
        mSuggestionList.setAdapter(mSuggestionAdapter);
    }

    @Override
    public void onLocalQueueChange(Bucket<Preferences> bucket, Set<String> queuedObjects) {

    }

    @Override
    public void onSyncObject(Bucket<Preferences> bucket, String key) {

    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when action mode is created.
         */
        void onActionModeCreated();

        /**
         * Callback for when action mode is destroyed.
         */
        void onActionModeDestroyed();

        /**
         * Callback for when a note has been selected.
         */
        void onNoteSelected(String noteID, String matchOffsets, boolean isMarkdownEnabled, boolean isPreviewEnabled);
    }

    // view holder for NotesCursorAdapter
    private static class NoteViewHolder {
        private ImageView mHasCollaborators;
        private ImageView mPinned;
        private ImageView mPublished;
        private TextView mContent;
        private TextView mDate;
        private TextView mTitle;
        private String mMatchOffsets;
        private String mNoteId;
        private View mStatus;

        public String getNoteId() {
            return mNoteId;
        }

        public void setNoteId(String noteId) {
            mNoteId = noteId;
        }
    }

    public class NotesCursorAdapter extends CursorAdapter {
        private ObjectCursor<Note> mCursor;

        private SearchSnippetFormatter.SpanFactory mSnippetHighlighter = new TextHighlighter(requireActivity(),
            R.attr.listSearchHighlightForegroundColor, R.attr.listSearchHighlightBackgroundColor);

        public NotesCursorAdapter(Context context, ObjectCursor<Note> c, int flags) {
            super(context, c, flags);
            mCursor = c;
        }

        public void changeCursor(ObjectCursor<Note> cursor) {
            mCursor = cursor;
            super.changeCursor(cursor);
        }

        @Override
        public Note getItem(int position) {
            mCursor.moveToPosition(position - mList.getHeaderViewsCount());
            return mCursor.getObject();
        }

        /*
         *  nbradbury - implemented "holder pattern" to boost performance with large note lists
         */
        @Override
        @SuppressLint("Range")
        public View getView(final int position, View view, ViewGroup parent) {
            final NoteViewHolder holder;

            if (view == null) {
                view = View.inflate(requireActivity().getBaseContext(), R.layout.note_list_row, null);
                holder = new NoteViewHolder();
                holder.mTitle = view.findViewById(R.id.note_title);
                holder.mContent = view.findViewById(R.id.note_content);
                holder.mDate = view.findViewById(R.id.note_date);
                holder.mHasCollaborators = view.findViewById(R.id.note_shared);
                holder.mPinned = view.findViewById(R.id.note_pinned);
                holder.mPublished = view.findViewById(R.id.note_published);
                holder.mStatus = view.findViewById(R.id.note_status);
                view.setTag(holder);
            } else {
                holder = (NoteViewHolder) view.getTag();
            }

            if (holder.mTitle.getTextSize() != mTitleFontSize) {
                holder.mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTitleFontSize);
                holder.mContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPreviewFontSize);
                holder.mDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPreviewFontSize);
            }

            if (position == getListView().getCheckedItemPosition()) {
                view.setActivated(true);
            } else {
                view.setActivated(false);
            }

            // for performance reasons we are going to get indexed values
            // from the cursor instead of instantiating the entire bucket object
            holder.mContent.setVisibility(mIsCondensedNoteList ? View.GONE : View.VISIBLE);
            mCursor.moveToPosition(position);
            holder.setNoteId(mCursor.getSimperiumKey());
            Calendar date = getDateByPreference(mCursor.getObject());
            holder.mDate.setText(date != null ? DateTimeUtils.getDateTextNumeric(date) : "");
            holder.mDate.setVisibility(mIsSearching && date != null ? View.VISIBLE : View.GONE);
            boolean hasCollaborators = mCursor.getObject().hasCollaborators();
            holder.mHasCollaborators.setVisibility(!hasCollaborators || mIsSearching ? View.GONE : View.VISIBLE);
            boolean isPinned = mCursor.getObject().isPinned();
            holder.mPinned.setVisibility(!isPinned || mIsSearching ? View.GONE : View.VISIBLE);
            boolean isPublished = !mCursor.getObject().getPublishedUrl().isEmpty();
            holder.mPublished.setVisibility(!isPublished || mIsSearching ? View.GONE : View.VISIBLE);
            boolean showIcons = isPinned || isPublished || hasCollaborators;
            boolean showDate = mIsSearching && date != null;
            holder.mStatus.setVisibility(showIcons || showDate ? View.VISIBLE : View.GONE);
            String title = mCursor.getString(mCursor.getColumnIndexOrThrow(Note.TITLE_INDEX_NAME));

            if (TextUtils.isEmpty(title)) {
                SpannableString newNoteString = new SpannableString(getString(R.string.new_note_list));
                newNoteString.setSpan(new TextAppearanceSpan(getActivity(), R.style.UntitledNoteAppearance),
                    0,
                    newNoteString.length(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                newNoteString.setSpan(new AbsoluteSizeSpan(mTitleFontSize, true),
                    0,
                    newNoteString.length(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                holder.mTitle.setText(newNoteString);
            } else {
                SpannableStringBuilder titleChecklistString = new SpannableStringBuilder(title);
                titleChecklistString = (SpannableStringBuilder) ChecklistUtils.addChecklistSpansForRegexAndColor(
                    getContext(),
                    titleChecklistString,
                    ChecklistUtils.CHECKLIST_REGEX,
                    ThemeUtils.getThemeTextColorId(getContext()),
                    true
                );
                holder.mTitle.setText(titleChecklistString);
            }

            holder.mMatchOffsets = null;
            int matchOffsetsIndex = -1;

            try {
                matchOffsetsIndex = mCursor.getColumnIndexOrThrow("match_offsets");
            } catch (IllegalArgumentException ignored) {}

            if (hasSearchQuery() && matchOffsetsIndex != -1) {
                title = mCursor.getString(mCursor.getColumnIndexOrThrow(Note.MATCHED_TITLE_INDEX_NAME));
                String snippet = mCursor.getString(mCursor.getColumnIndexOrThrow(Note.MATCHED_CONTENT_INDEX_NAME));
                holder.mMatchOffsets = mCursor.getString(matchOffsetsIndex);

                try {
                    holder.mContent.setText(SearchSnippetFormatter.formatString(
                        getContext(),
                        snippet,
                        mSnippetHighlighter,
                        R.color.text_title_disabled));
                    holder.mTitle.setText(SearchSnippetFormatter.formatString(
                        getContext(),
                        title,
                        mSnippetHighlighter, ThemeUtils.getThemeTextColorId(getContext())));
                } catch (NullPointerException e) {
                    title = StrUtils.notNullStr(mCursor.getString(mCursor.getColumnIndexOrThrow(Note.TITLE_INDEX_NAME)));
                    holder.mTitle.setText(title);
                    String matchedContentPreview = StrUtils.notNullStr(mCursor.getString(mCursor.getColumnIndexOrThrow(Note.CONTENT_PREVIEW_INDEX_NAME)));
                    holder.mContent.setText(matchedContentPreview);
                }
            } else if (!mIsCondensedNoteList) {
                String contentPreview = mCursor.getString(mCursor.getColumnIndexOrThrow(Note.CONTENT_PREVIEW_INDEX_NAME));

                if (title == null || title.equals(contentPreview) || title.equals(getString(R.string.new_note_list))) {
                    holder.mContent.setVisibility(View.GONE);
                } else {
                    holder.mContent.setText(contentPreview);
                    SpannableStringBuilder checklistString = new SpannableStringBuilder(contentPreview);
                    checklistString = (SpannableStringBuilder) ChecklistUtils.addChecklistSpansForRegexAndColor(
                        getContext(),
                        checklistString,
                        ChecklistUtils.CHECKLIST_REGEX,
                        R.color.text_title_disabled,
                        true
                    );
                    holder.mContent.setText(checklistString);
                }
            }

            // Add mouse right click support for showing a popup menu
            view.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY && event.getAction() == MotionEvent.ACTION_DOWN) {
                        showPopupMenuAtPosition(view, position);
                        return true;
                    }

                    return false;
                }
            });

            return view;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        }
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Preferences> bucket, Preferences object) {
    }

    @Override
    public void onDeleteObject(Bucket<Preferences> bucket, Preferences object) {
        if (isAdded()) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getSearchItems();
                }
            });
        }
    }

    @Override
    public void onNetworkChange(Bucket<Preferences> bucket, Bucket.ChangeType type, String key) {
        if (isAdded()) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getSearchItems();
                }
            });
        }
    }

    @Override
    public void onSaveObject(Bucket<Preferences> bucket, Preferences object) {
        if (isAdded()) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getSearchItems();
                }
            });
        }
    }

    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private final List<Suggestion> mSuggestions;

        private SuggestionAdapter(List<Suggestion> suggestions) {
            mSuggestions = new ArrayList<>(suggestions);
        }

        @Override
        public int getItemCount() {
            return mSuggestions.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mSuggestions.get(position).getType();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            switch (holder.mViewType) {
                case HISTORY:
                    holder.mSuggestionText.setText(mSuggestions.get(position).getName());
                    holder.mSuggestionIcon.setImageResource(R.drawable.ic_history_24dp);
                    holder.mButtonDelete.setVisibility(View.VISIBLE);
                    break;
                case QUERY:
                    holder.mSuggestionText.setText(mSuggestions.get(position).getName());
                    holder.mSuggestionIcon.setImageResource(R.drawable.ic_search_24dp);
                    holder.mButtonDelete.setVisibility(View.GONE);
                    break;
                case TAG:
                    holder.mSuggestionText.setText(TAG_PREFIX + mSuggestions.get(position).getName());
                    holder.mSuggestionIcon.setImageResource(R.drawable.ic_tag_24dp);
                    holder.mButtonDelete.setVisibility(View.GONE);
                    break;
            }

            holder.mButtonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!isAdded()) {
                        return;
                    }

                    final String item = holder.mSuggestionText.getText().toString();
                    deleteSearchItem(item);
                    Snackbar
                        .make(getRootView(), R.string.snackbar_deleted_recent_search, Snackbar.LENGTH_LONG)
                        .setAction(
                            getString(R.string.undo),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    addSearchItem(item, mDeletedItemIndex);
                                }
                            }
                        )
                        .show();
                }
            });
            holder.mButtonDelete.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (v.isHapticFeedbackEnabled()) {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    }

                    Toast.makeText(getContext(), requireContext().getString(R.string.description_delete_item), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((NotesActivity) requireActivity()).submitSearch(holder.mSuggestionText.getText().toString());

                    if (holder.mViewType == HISTORY) {
                        AnalyticsTracker.track(
                            RECENT_SEARCH_TAPPED,
                            CATEGORY_SEARCH,
                            "recent_search_tapped"
                        );
                    }
                }
            });
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.search_suggestion, parent, false), viewType);
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private ImageButton mButtonDelete;
            private ImageView mSuggestionIcon;
            private TextView mSuggestionText;
            private View mView;
            private int mViewType;

            private ViewHolder(View itemView, int viewType) {
                super(itemView);
                mView = itemView;
                mViewType = viewType;
                mSuggestionText = itemView.findViewById(R.id.suggestion_text);
                mSuggestionIcon = itemView.findViewById(R.id.suggestion_icon);
                mButtonDelete = itemView.findViewById(R.id.suggestion_delete);
            }
        }

        private void updateItems(List<Suggestion> suggestions) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SuggestionDiffCallback(mSuggestions, suggestions));
            mSuggestions.clear();
            mSuggestions.addAll(suggestions);
            diffResult.dispatchUpdatesTo(this);
        }
    }

    private class SuggestionDiffCallback extends DiffUtil.Callback {
        private List<Suggestion> mListNew;
        private List<Suggestion> mListOld;

        private SuggestionDiffCallback(List<Suggestion> oldList, List<Suggestion> newList) {
            mListOld = oldList;
            mListNew = newList;
        }

        @Override
        public boolean areContentsTheSame(int itemPositionOld, int itemPositionNew) {
            Suggestion itemOld = mListOld.get(itemPositionOld);
            Suggestion itemNew = mListNew.get(itemPositionNew);
            return itemOld.getName().equalsIgnoreCase(itemNew.getName());
        }

        @Override
        public boolean areItemsTheSame(int itemPositionOld, int itemPositionNew) {
            Suggestion itemOld = mListOld.get(itemPositionOld);
            Suggestion itemNew = mListNew.get(itemPositionNew);
            return itemOld.getName().equalsIgnoreCase(itemNew.getName());
        }

        @Override
        public int getNewListSize() {
            return mListNew.size();
        }

        @Override
        public int getOldListSize() {
            return mListOld.size();
        }
    }

    private Calendar getDateByPreference(Note note) {
        switch (PrefUtils.getIntPref(requireContext(), PrefUtils.PREF_SORT_ORDER)) {
            case DATE_CREATED_ASCENDING:
            case DATE_CREATED_DESCENDING:
                return note.getCreationDate();
            case DATE_MODIFIED_ASCENDING:
            case DATE_MODIFIED_DESCENDING:
                return note.getModificationDate();
            case ALPHABETICAL_ASCENDING:
            case ALPHABETICAL_DESCENDING:
            default:
                return null;
        }
    }

    private void showPopupMenuAtPosition(View view, int position) {
        if (view.getContext() == null) {
            return;
        }

        final Note note = mNotesAdapter.getItem(position + mList.getHeaderViewsCount());
        if (note == null) {
            return;
        }

        PopupMenu popup = new PopupMenu(view.getContext(), view, Gravity.END);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.bulk_edit, popup.getMenu());

        if (!getListView().isLongClickable()) {
            // If viewing the trash, remove pin menu item and change trash menu title to 'Restore'
            popup.getMenu().removeItem(R.id.menu_pin);
            if (popup.getMenu().getItem(POPUP_MENU_FIRST_ITEM_POSITION) != null) {
                popup.getMenu().getItem(POPUP_MENU_FIRST_ITEM_POSITION).setTitle(R.string.restore);
            }
        } else if (popup.getMenu().getItem(POPUP_MENU_FIRST_ITEM_POSITION) != null) {
            // If not viewing the trash, set pin menu title based on note pin state
            int pinTitle = note.isPinned() ? R.string.unpin_from_top : R.string.pin_to_top;
            popup.getMenu().getItem(POPUP_MENU_FIRST_ITEM_POSITION).setTitle(pinTitle);
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_pin:
                        note.setPinned(!note.isPinned());
                        note.setModificationDate(Calendar.getInstance());
                        note.save();
                        refreshList();
                        return true;
                    case R.id.menu_trash:
                        if (getActivity() != null) {
                            ((NotesActivity) getActivity()).trashNote(note);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        popup.show();
    }

    private static class RefreshListTask extends AsyncTask<Boolean, Void, ObjectCursor<Note>> {
        private SoftReference<NoteListFragment> mNoteListFragmentReference;
        private boolean mIsFromNavSelect;

        private RefreshListTask(NoteListFragment context) {
            mNoteListFragmentReference = new SoftReference<>(context);
        }

        @Override
        protected ObjectCursor<Note> doInBackground(Boolean... args) {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            mIsFromNavSelect = args[0];
            return fragment.queryNotes();
        }

        @Override
        protected void onPostExecute(ObjectCursor<Note> cursor) {
            NoteListFragment fragment = mNoteListFragmentReference.get();

            if (cursor == null || fragment.getActivity() == null || fragment.getActivity().isFinishing()) {
                return;
            }

            // While using a Query.FullTextMatch it's easy to enter an invalid term so catch the error and clear the cursor
            int count;

            try {
                fragment.mNotesAdapter.changeCursor(cursor);
                count = fragment.mNotesAdapter.getCount();
            } catch (SQLiteException e) {
                count = 0;
                Log.e(Simplenote.TAG, "Invalid SQL statement", e);
                fragment.mNotesAdapter.changeCursor(null);
            }

            NotesActivity notesActivity = (NotesActivity) fragment.getActivity();

            if (notesActivity != null) {
                if (mIsFromNavSelect && DisplayUtils.isLargeScreenLandscape(notesActivity)) {
                    if (count == 0) {
                        notesActivity.showDetailPlaceholder();
                    } else {
                        // Select the first note
                        fragment.selectFirstNote();
                    }
                }

                notesActivity.updateTrashMenuItem(true);
            }

            if (fragment.mSelectedNoteId != null) {
                fragment.setNoteSelected(fragment.mSelectedNoteId);
                fragment.mSelectedNoteId = null;
            }
        }
    }

    private static class RefreshListForSearchTask extends AsyncTask<Void, Void, ObjectCursor<Note>> {
        private SoftReference<NoteListFragment> mNoteListFragmentReference;

        private RefreshListForSearchTask(NoteListFragment context) {
            mNoteListFragmentReference = new SoftReference<>(context);
        }

        @Override
        protected ObjectCursor<Note> doInBackground(Void... args) {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            return fragment.queryNotesForSearch();
        }

        @Override
        protected void onPostExecute(ObjectCursor<Note> cursor) {
            NoteListFragment fragment = mNoteListFragmentReference.get();

            if (cursor == null || fragment.getActivity() == null || fragment.getActivity().isFinishing()) {
                return;
            }

            // While using Query.FullTextMatch, it's easy to enter an invalid term so catch the error and clear the cursor.
            try {
                fragment.mNotesAdapter.changeCursor(cursor);
            } catch (SQLiteException e) {
                Log.e(Simplenote.TAG, "Invalid SQL statement", e);
                fragment.mNotesAdapter.changeCursor(null);
            }

            NotesActivity notesActivity = (NotesActivity) fragment.requireActivity();
            notesActivity.updateTrashMenuItem(true);

            if (fragment.mSelectedNoteId != null) {
                fragment.setNoteSelected(fragment.mSelectedNoteId);
                fragment.mSelectedNoteId = null;
            }
        }
    }

    private static class PinNotesTask extends AsyncTask<Void, Void, Void> {
        private SoftReference<NoteListFragment> mNoteListFragmentReference;
        private SparseBooleanArray mSelectedRows = new SparseBooleanArray();

        private PinNotesTask(NoteListFragment context) {
            mNoteListFragmentReference = new SoftReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            mSelectedRows = fragment.getListView().getCheckedItemPositions();
        }

        @Override
        protected Void doInBackground(Void... args) {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            // Get the checked notes and add them to the pinnedNotesList
            // We can't modify the note in this loop because the adapter could change
            List<Note> pinnedNotesList = new ArrayList<>();

            for (int i = 0; i < mSelectedRows.size(); i++) {
                if (mSelectedRows.valueAt(i)) {
                    pinnedNotesList.add(fragment.mNotesAdapter.getItem(mSelectedRows.keyAt(i)));
                }
            }

            // Now loop through the notes list and mark them as pinned
            for (Note pinnedNote : pinnedNotesList) {
                pinnedNote.setPinned(!pinnedNote.isPinned());
                pinnedNote.setModificationDate(Calendar.getInstance());
                pinnedNote.save();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            fragment.mActionMode.finish();
            fragment.refreshList();
        }
    }

    private static class TrashNotesTask extends AsyncTask<Void, Void, Void> {
        private List<String> mDeletedNoteIds = new ArrayList<>();
        private SoftReference<NoteListFragment> mNoteListFragmentReference;
        private SparseBooleanArray mSelectedRows = new SparseBooleanArray();

        private TrashNotesTask(NoteListFragment context) {
            mNoteListFragmentReference = new SoftReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            mSelectedRows = fragment.getListView().getCheckedItemPositions();
        }

        @Override
        protected Void doInBackground(Void... args) {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            // Get the checked notes and add them to the deletedNotesList
            // We can't modify the note in this loop because the adapter could change
            List<Note> deletedNotesList = new ArrayList<>();

            for (int i = 0; i < mSelectedRows.size(); i++) {
                if (mSelectedRows.valueAt(i)) {
                    deletedNotesList.add(fragment.mNotesAdapter.getItem(mSelectedRows.keyAt(i)));
                }
            }

            // Now loop through the notes list and mark them as deleted
            for (Note deletedNote : deletedNotesList) {
                mDeletedNoteIds.add(deletedNote.getSimperiumKey());
                deletedNote.setDeleted(!deletedNote.isDeleted());
                deletedNote.setModificationDate(Calendar.getInstance());
                deletedNote.save();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            NoteListFragment fragment = mNoteListFragmentReference.get();
            NotesActivity notesActivity = ((NotesActivity) fragment.getActivity());

            if (notesActivity != null) {
                notesActivity.showUndoBarWithNoteIds(mDeletedNoteIds);
            }

            if (!fragment.isDetached()) {
                fragment.updateSelectionAfterTrashAction();
                fragment.mActionMode.finish();
                fragment.refreshList();
            }
        }
    }
}
