package com.automattic.simplenote;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.SearchSnippetFormatter;
import com.automattic.simplenote.utils.SearchTokenizer;
import com.automattic.simplenote.utils.StrUtils;
import com.automattic.simplenote.utils.TextHighlighter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.Query;
import com.simperium.client.Query.SortType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A list fragment representing a list of Notes. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link NoteEditorFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends ListFragment implements AdapterView.OnItemLongClickListener, AbsListView.MultiChoiceModeListener {

    private ActionMode mActionMode;

	protected NotesCursorAdapter mNotesAdapter;
    private TextView mEmptyListTextView;
    private LinearLayout mDividerShadow;
	private int mNumPreviewLines;
    protected String mSearchString;
    private ViewSwitcher mWelcomeViewSwitcher;
    private String mSelectedNoteId;
    private refreshListTask mRefreshListTask;

    private int mTitleFontSize;
    private int mPreviewFontSize;

    private Tracker mTracker;

	/**
	 * The preferences key representing the activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String TAG_BUTTON_SIGNIN = "sign_in";
    private static final String TAG_BUTTON_SIGNUP = "sign_up";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

    public void setEmptyListViewClickable(boolean isClickable) {
        if (mEmptyListTextView != null) {
            mEmptyListTextView.setClickable(isClickable);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setItemChecked(position, true);
        if (mActionMode == null)
            getActivity().startActionMode(this);
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.bulk_edit, menu);
        mActionMode = actionMode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (getListView().getCheckedItemIds().length > 0 && item.getItemId() == R.id.menu_delete)
            new trashNotesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    NotesActivity notesActivity = (NotesActivity) getActivity();
                    setActivateOnItemClick(DisplayUtils.isLargeLandscape(notesActivity));
                    notesActivity.showDetailPlaceholder();
                }
            }
        });
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
        int checkedCount = getListView().getCheckedItemCount();
        if (checkedCount == 0)
            actionMode.setTitle("");
        else
            actionMode.setTitle(getResources().getQuantityString(R.plurals.selected_notes, checkedCount, checkedCount));
    }

    /**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when a note has been selected.
		 */
		public void onNoteSelected(String noteID, int position, boolean isNew, String matchOffsets);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sCallbacks = new Callbacks() {
		@Override
		public void onNoteSelected(String noteID, int position, boolean isNew, String matchOffsets) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mNotesAdapter = new NotesCursorAdapter(getActivity().getBaseContext(), null, 0);
		setListAdapter(mNotesAdapter);
	}


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Simplenote application = (Simplenote) getActivity().getApplication();
        mTracker = application.getTracker();
    }

    // nbradbury - load values from preferences
	protected void getPrefs() {
        boolean condensedList = PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_CONDENSED_LIST, false);
		mNumPreviewLines = (condensedList) ? 0 : 2;
        mPreviewFontSize = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_FONT_SIZE, 18);
        mTitleFontSize = Math.round(mPreviewFontSize + mPreviewFontSize * 0.222f);
	}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_notes_list, container, false);
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        NotesActivity notesActivity = (NotesActivity)getActivity();

        LinearLayout emptyView = (LinearLayout)view.findViewById(android.R.id.empty);
        emptyView.setVisibility(View.GONE);
        mEmptyListTextView = (TextView)view.findViewById(R.id.empty_message);
        mEmptyListTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNote();
            }
        });
        setEmptyListMessage("<strong>" + getString(R.string.no_notes_here) + "</strong><br />" + String.format(getString(R.string.why_not_create_one), "<u>", "</u>"));
        mDividerShadow = (LinearLayout)view.findViewById(R.id.divider_shadow);
        mWelcomeViewSwitcher = (ViewSwitcher)view.findViewById(R.id.welcome_view_switcher);

        TextView welcomeTextView = (TextView)view.findViewById(R.id.welcome_textview);
        welcomeTextView.setText(Html.fromHtml(getString(R.string.welcome_want_more) + " <u>" + getString(R.string.use_simplenote_account) + "</u> &raquo;"));
        TextView laterTextView = (TextView)view.findViewById(R.id.welcome_later_textview);
        laterTextView.setText(Html.fromHtml(getString(R.string.maybe_later) + ", <u>" + getString(R.string.just_try_app) + "</u> &raquo;"));

        TextView signInButton = (TextView)view.findViewById(R.id.welcome_sign_in);
        signInButton.setTag(TAG_BUTTON_SIGNIN);
        signInButton.setOnClickListener(signInClickListener);
        TextView signUpButton = (TextView)view.findViewById(R.id.welcome_sign_up);
        signUpButton.setTag(TAG_BUTTON_SIGNUP);
        signUpButton.setOnClickListener(signInClickListener);

        if (DisplayUtils.isLargeLandscape(notesActivity)) {
            setActivateOnItemClick(true);
            mDividerShadow.setVisibility(View.VISIBLE);
        }

        welcomeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeViewSwitcher.showNext();
            }
        });
        laterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeViewSwitcher.showNext();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PrefUtils.PREF_APP_TRIAL, true);
                editor.apply();
            }
        });

        getListView().setOnItemLongClickListener(this);
        getListView().setMultiChoiceModeListener(this);
	}

	@Override
	public void onAttach(Activity activity) {
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

        setWelcomeViewVisibility();

        refreshList();

        if (!PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_APP_TRIAL, false)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mWelcomeViewSwitcher.showNext();
                }
            }, 100);
        }
	}

    public void setWelcomeViewVisibility() {
        if (mWelcomeViewSwitcher != null && getActivity() != null) {
            Simplenote currentApp = (Simplenote) getActivity().getApplication();
            if (currentApp.getSimperium().needsAuthorization()) {
                int bottomMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) getListView().getLayoutParams();
                mlp.setMargins(0, 0, 0, bottomMargin);
                getListView().getEmptyView().setLayoutParams(mlp);
                mWelcomeViewSwitcher.setVisibility(View.VISIBLE);
            } else {
                mWelcomeViewSwitcher.setVisibility(View.GONE);
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) getListView().getLayoutParams();
                mlp.setMargins(0, 0, 0, 0);
                getListView().getEmptyView().setLayoutParams(mlp);
            }
        }
    }

    public void hideWelcomeView() {
        if (mWelcomeViewSwitcher != null)
            mWelcomeViewSwitcher.setVisibility(View.GONE);
    }

    private Button.OnClickListener signInClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((NotesActivity)getActivity()).startLoginActivity(v.getTag().equals(TAG_BUTTON_SIGNIN));
        }
    };

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sCallbacks;
	}

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    public void setEmptyListMessage(String message) {
        if (mEmptyListTextView != null && message != null)
            mEmptyListTextView.setText(Html.fromHtml(message));
    }

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
        if (!isAdded()) return;
		super.onListItemClick(listView, view, position, id);

        NoteViewHolder holder = (NoteViewHolder)view.getTag();
        String noteID = holder.getNoteId();

        if (DisplayUtils.isLargeLandscape(getActivity())) {
            if (noteID != null) {
                mCallbacks.onNoteSelected(noteID, position, false, holder.matchOffsets);
            }
        } else {
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, noteID);

            Intent editNoteIntent = new Intent(getActivity(), NoteEditorActivity.class);
            editNoteIntent.putExtras(arguments);

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), view, "transition_editor");
            ActivityCompat.startActivityForResult(getActivity(), editNoteIntent, Simplenote.INTENT_EDIT_NOTE, options.toBundle());
        }

        mActivatedPosition = position;

        mTracker.send(
                new HitBuilders.EventBuilder()
                        .setCategory("note")
                        .setAction("viewed_note")
                        .setLabel("note_list_row_tap")
                        .build()
        );
    }

    /**
     * Selects first row in the list if available
     */
    public void selectFirstNote() {
        if (mNotesAdapter.getCount() > 0) {
            Note selectedNote = mNotesAdapter.getItem(0);
            mCallbacks.onNoteSelected(selectedNote.getSimperiumKey(), 0, false, null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
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
        if (visible)
            mDividerShadow.setVisibility(View.VISIBLE);
        else
            mDividerShadow.setVisibility(View.GONE);
    }

	public void refreshList() {
        refreshList(false);
	}

    public void refreshList(boolean fromNav) {
        if (mRefreshListTask != null && mRefreshListTask.getStatus() != AsyncTask.Status.FINISHED)
            mRefreshListTask.cancel(true);

        mRefreshListTask = new refreshListTask();
        mRefreshListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fromNav);
    }

    public void refreshListFromNavSelect() {
        refreshList(true);
    }

    public ObjectCursor<Note> queryNotes(){
        NotesActivity notesActivity = (NotesActivity)getActivity();
        Query<Note> query = notesActivity.getSelectedTag().query();

        if (hasSearchQuery()) {
            query.where(new Query.FullTextMatch(new SearchTokenizer(mSearchString)));
            query.include(new Query.FullTextOffsets("match_offsets"));
            query.include(new Query.FullTextSnippet(Note.MATCHED_TITLE_INDEX_NAME, Note.TITLE_INDEX_NAME));
            query.include(new Query.FullTextSnippet(Note.MATCHED_CONTENT_INDEX_NAME, Note.CONTENT_PROPERTY));
            query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        } else {
            query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        }

        query.include(Note.PINNED_INDEX_NAME);

        sortNoteQuery(query);

        return query.execute();
    }

	public void addNote() {

        // Prevents jarring 'New note...' from showing in the list view when creating a new note
        NotesActivity notesActivity = (NotesActivity)getActivity();
        if (!DisplayUtils.isLargeLandscape(notesActivity))
            notesActivity.stopListeningToNotesBucket();

		// Create & save new note
		Simplenote simplenote = (Simplenote) getActivity().getApplication();
		Bucket<Note> notesBucket = simplenote.getNotesBucket();
		Note note = notesBucket.newObject();
        note.setCreationDate(Calendar.getInstance());
        note.setModificationDate(note.getCreationDate());

        if (notesActivity.getSelectedTag() != null && notesActivity.getSelectedTag().name != null) {
            String tagName = notesActivity.getSelectedTag().name;
            if (!tagName.equals(getString(R.string.notes)) && !tagName.equals(getString(R.string.trash)))
                note.setTagString(tagName);
        }

		note.save();

        if (DisplayUtils.isLargeLandscape(getActivity())) {
            mCallbacks.onNoteSelected(note.getSimperiumKey(), 0, true, null);
        } else {
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, true);
            Intent editNoteIntent = new Intent(getActivity(), NoteEditorActivity.class);
            editNoteIntent.putExtras(arguments);

            getActivity().startActivityForResult(editNoteIntent, Simplenote.INTENT_EDIT_NOTE);
        }
	}

    public void setNoteSelected(String selectedNoteID) {
        // Loop through notes and set note selected if found
        ObjectCursor<Note> cursor = (ObjectCursor<Note>) mNotesAdapter.getCursor();
        if (cursor != null) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String noteKey = cursor.getSimperiumKey();
                if (noteKey != null && noteKey.equals(selectedNoteID)) {
                    setActivatedPosition(i);
                    return;
                }
            }
        }

        // Didn't find the note, let's try again after the cursor updates (see refreshListTask)
        mSelectedNoteId = selectedNoteID;
    }

	public class NotesCursorAdapter extends CursorAdapter {
        private ObjectCursor<Note> mCursor;

        private SearchSnippetFormatter.SpanFactory mSnippetHighlighter = new TextHighlighter(getActivity(),
            R.attr.listSearchHighlightForegroundColor, R.attr.listSearchHighlightBackgroundColor);

        public NotesCursorAdapter(Context context, ObjectCursor<Note> c, int flags) {
            super(context, c, flags);
            mCursor = c;
        }

        public void changeCursor(ObjectCursor<Note> cursor){
            mCursor = cursor;
            super.changeCursor(cursor);
        }

        @Override
        public Note getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getObject();
        }

        /*
        *  nbradbury - implemented "holder pattern" to boost performance with large note lists
        */
		@Override
		public View getView(int position, View view, ViewGroup parent) {

			NoteViewHolder holder;
			if (view == null) {
				view = View.inflate(getActivity().getBaseContext(), R.layout.note_list_row, null);
				holder = new NoteViewHolder();
				holder.titleTextView = (TextView) view.findViewById(R.id.note_title);
				holder.contentTextView = (TextView) view.findViewById(R.id.note_content);
				holder.pinImageView = (ImageView) view.findViewById(R.id.note_pin);
				view.setTag(holder);
			} else {
				holder = (NoteViewHolder) view.getTag();
			}

            if (holder.titleTextView.getTextSize() != mTitleFontSize) {
                holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTitleFontSize);
                holder.contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPreviewFontSize);
            }

            if (position == getListView().getCheckedItemPosition())
                view.setActivated(true);
            else
                view.setActivated(false);

            // for performance reasons we are going to get indexed values
            // from the cursor instead of instantiating the entire bucket object
            holder.contentTextView.setVisibility(View.VISIBLE);
            holder.contentTextView.setMaxLines(mNumPreviewLines);
            mCursor.moveToPosition(position);
            holder.setNoteId(mCursor.getSimperiumKey());
            int pinned = mCursor.getInt(mCursor.getColumnIndex(Note.PINNED_INDEX_NAME));
            holder.pinImageView.setVisibility(pinned == 1 ? View.VISIBLE : View.GONE);

            String title = mCursor.getString(mCursor.getColumnIndex(Note.TITLE_INDEX_NAME));

            if (title == null || title.equals("")) {
                SpannableString untitled = new SpannableString(getString(R.string.new_note_list));
                untitled.setSpan(new TextAppearanceSpan(getActivity(), R.style.UntitledNoteAppearance), 0, untitled.length(), 0x0);
                holder.titleTextView.setText(untitled);
            } else {
                holder.titleTextView.setText(title);
            }

            holder.matchOffsets = null;

            int matchOffsetsIndex = mCursor.getColumnIndex("match_offsets");
            if (hasSearchQuery() && matchOffsetsIndex != -1) {
                title = mCursor.getString(mCursor.getColumnIndex(Note.MATCHED_TITLE_INDEX_NAME));
                String snippet = mCursor.getString(mCursor.getColumnIndex(Note.MATCHED_CONTENT_INDEX_NAME));

                holder.matchOffsets = mCursor.getString(matchOffsetsIndex);

                try {
                    holder.contentTextView.setText(SearchSnippetFormatter.formatString(snippet, mSnippetHighlighter));
                    holder.titleTextView.setText(SearchSnippetFormatter.formatString(title, mSnippetHighlighter));
                } catch (NullPointerException e) {
                    title = StrUtils.notNullStr(mCursor.getString(mCursor.getColumnIndex(Note.TITLE_INDEX_NAME)));
                    holder.titleTextView.setText(title);
                    String matchedContentPreview = StrUtils.notNullStr(mCursor.getString(mCursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME)));
                    holder.contentTextView.setText(matchedContentPreview);
                }
            } else if (mNumPreviewLines > 0) {
                String contentPreview = mCursor.getString(mCursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME));
                if (title == null || title.equals(contentPreview) || title.equals(getString(R.string.new_note_list)))
                    holder.contentTextView.setVisibility(View.GONE);
                else
                    holder.contentTextView.setText(contentPreview);
            }
			
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

	// view holder for NotesCursorAdapter
	private static class NoteViewHolder {
		TextView titleTextView;
		TextView contentTextView;
		ImageView pinImageView;
        public String matchOffsets;
        private String mNoteId;

        public void setNoteId(String noteId) {
            mNoteId = noteId;
        }

        public String getNoteId() {
            return mNoteId;
        }
	}

	public void searchNotes(String searchString) {
        if (!searchString.equals(mSearchString)){
            mSearchString = searchString;
            refreshList();
        }
	}

    /**
     * Clear search and load all notes
     */
    public void clearSearch() {
        if (mSearchString != null && !mSearchString.equals("")){
            mSearchString = null;
            refreshList();
        }
    }

    public boolean hasSearchQuery(){
        return mSearchString != null && !mSearchString.equals("");
    }

    public void sortNoteQuery(Query<Note> noteQuery){
        noteQuery.order("pinned", SortType.DESCENDING);
		int sortPref = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_SORT_ORDER);
		switch (sortPref) {
        case 0:
            noteQuery.order(Note.MODIFIED_INDEX_NAME, SortType.DESCENDING);
            break;
		case 1:
            noteQuery.order(Note.MODIFIED_INDEX_NAME, SortType.ASCENDING);
			break;
		case 2:
            noteQuery.order(Note.CREATED_INDEX_NAME, SortType.DESCENDING);
			break;
		case 3:
            noteQuery.order(Note.CREATED_INDEX_NAME, SortType.ASCENDING);
			break;
		case 4:
            noteQuery.order(Note.CONTENT_PROPERTY, SortType.ASCENDING);
			break;
		case 5:
            noteQuery.order(Note.CONTENT_PROPERTY, SortType.DESCENDING);
			break;
		}
    }

    private class refreshListTask extends AsyncTask<Boolean, Void, ObjectCursor<Note>> {
        boolean mIsFromNavSelect;

        @Override
        protected ObjectCursor<Note> doInBackground(Boolean ... args) {
            mIsFromNavSelect = args[0];
            return queryNotes();
        }

        @Override
        protected void onPostExecute(ObjectCursor<Note> cursor) {
            if (getActivity() == null || getActivity().isFinishing())
                return;

            // While using a Query.FullTextMatch it's easy to enter an invalid term so catch the error and clear the cursor
            int count;
            try {
                mNotesAdapter.changeCursor(cursor);
                count = mNotesAdapter.getCount();
            } catch (SQLiteException e) {
                count = 0;
                android.util.Log.e(Simplenote.TAG, "Invalid SQL statement", e);
                mNotesAdapter.changeCursor(null);
            }

            NotesActivity notesActivity = (NotesActivity)getActivity();
            if (notesActivity != null) {
                if (mIsFromNavSelect && DisplayUtils.isLargeLandscape(notesActivity)) {
                        if (count == 0) {
                            notesActivity.showDetailPlaceholder();
                        } else {
                            // Select the first note
                            selectFirstNote();
                        }
                }
                notesActivity.updateTrashMenuItem();
            }

            if (mSelectedNoteId != null) {
                setNoteSelected(mSelectedNoteId);
                mSelectedNoteId = null;
            }
        }
    }

    private class trashNotesTask extends AsyncTask<Void, Void, Void> {

        List<String> deletedNotesIds = new ArrayList<String>();

        @Override
        protected Void doInBackground(Void... args) {
            SparseBooleanArray selectedRows = getListView().getCheckedItemPositions();

            // Get the checked notes and add them to the deletedNotesList
            // We can't modify the note in this loop because the adapter could change
            List<Note> deletedNotesList = new ArrayList<>();
            for (int i = 0; i < selectedRows.size(); i++) {
                if (selectedRows.valueAt(i)) {
                    deletedNotesList.add(mNotesAdapter.getItem(selectedRows.keyAt(i)));
                }
            }

            // Now loop through the notes list and mark them as deleted
            for (Note deletedNote : deletedNotesList) {
                deletedNotesIds.add(deletedNote.getSimperiumKey());
                deletedNote.setDeleted(!deletedNote.isDeleted());
                deletedNote.setModificationDate(Calendar.getInstance());
                deletedNote.save();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            NotesActivity notesActivity = ((NotesActivity) getActivity());
            if (notesActivity != null)
                notesActivity.showUndoBarWithNoteIds(deletedNotesIds);

            refreshList();
        }
    }
}
