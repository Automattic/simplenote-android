package com.automattic.simplenote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.Query;
import com.simperium.client.Query.SortType;

import java.util.Arrays;
import java.util.Calendar;
import java.util.prefs.Preferences;

/**
 * A list fragment representing a list of Notes. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link NoteEditorFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends ListFragment implements ActionBar.OnNavigationListener {

    private static final int NAVIGATION_ITEM_ALL_NOTES=0;
    private static final int NAVIGATION_ITEM_TRASH=1;

	private NotesCursorAdapter mNotesAdapter;
	private Bucket<Note> mNotesBucket;
	private Bucket<Tag> mTagsBucket;
    private TextView mEmptyListTextView;
    private LinearLayout mDividerShadow;
	private int mNumPreviewLines;
	private String[] mMenuItems;
    private String mSelectedTag;
    private String mSearchString;
	private int mNavigationItem;
    private boolean mNavListLoaded;
	/**
	 * The preferences key representing the activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

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
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when a note has been selected.
		 */
		public void onNoteSelected(Note note);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sCallbacks = new Callbacks() {
		@Override
		public void onNoteSelected(Note note) {
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

		Simplenote application = (Simplenote) getActivity().getApplication();
		mNotesBucket = application.getNotesBucket();
        mTagsBucket = application.getTagsBucket();

        ObjectCursor<Note> cursor = queryNotes();
		mNotesAdapter = new NotesCursorAdapter(getActivity().getBaseContext(), cursor, 0);
		setListAdapter(mNotesAdapter);

	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    // nbradbury - load values from preferences
	protected void getPrefs() {
		mNumPreviewLines = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_NUM_PREVIEW_LINES, 2);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.notes_list, container, false);
        return view;
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        NotesActivity notesActivity = (NotesActivity)getActivity();

        mEmptyListTextView = (TextView)view.findViewById(android.R.id.empty);
        mDividerShadow = (LinearLayout)view.findViewById(R.id.divider_shadow);

        if (notesActivity.isLargeScreenLandscape()) {
            setActivateOnItemClick(true);
            mDividerShadow.setVisibility(View.VISIBLE);
        }

        getListView().setDivider(getResources().getDrawable(R.drawable.list_divider));
        getListView().setDividerHeight(2);
        getListView().setBackgroundColor(getResources().getColor(R.color.white));

        updateMenuItems();
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
        mNavListLoaded = false;
        getPrefs();
        refreshList();
        updateMenuItems();
        // update the view again
        mTagsBucket.addListener(mTagsMenuUpdater);
	}

    @Override
    public void onPause(){
        super.onPause();
        mTagsBucket.removeListener(mTagsMenuUpdater);
    }

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

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		Note note = (Note)getListAdapter().getItem(position);
        if (note != null)
            mCallbacks.onNoteSelected(note);

        mActivatedPosition = position;
	}

    /**
     * Selects first row in the list if available
     */
    public void selectFirstNote() {
        if (mNotesAdapter.getCount() > 0) {
            Note selectedNote = mNotesAdapter.getItem(0);
            mCallbacks.onNoteSelected(selectedNote);
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

	private void setActivatedPosition(int position) {
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

	@SuppressWarnings("deprecation")
	public void refreshList() {
		Log.d(Simplenote.TAG, "Refresh the list");
        mNotesAdapter.changeCursor(queryNotes());
		mNotesAdapter.notifyDataSetChanged();
	}

    public ObjectCursor<Note> queryNotes(){
        Log.d(Simplenote.TAG, String.format("Querying %d %s", mNavigationItem, mSelectedTag));
        Query<Note> query = null;
        if (mSearchString != null) {
            query = Note.search(mNotesBucket, mSearchString);
        } else if (mNavigationItem == NAVIGATION_ITEM_ALL_NOTES) {
			// All notes
			query = Note.all(mNotesBucket);
		} else if (mNavigationItem == NAVIGATION_ITEM_TRASH) {
			// Trashed notes
			query = Note.allDeleted(mNotesBucket);
		} else {
			query = Note.allInTag(mNotesBucket, mSelectedTag);
		}
        query.include("title", "contentPreview", "pinned", "modificationDate");
        sortNoteQuery(query);
        return query.execute();
    }

	private void updateMenuItems() {
		// Update ActionBar menu
		Simplenote application = (Simplenote) getActivity().getApplication();
        Bucket<Tag> tagBucket = application.getTagsBucket();
        ObjectCursor<Tag> tagCursor = Tag.all(tagBucket).execute();
		String[] tags = new String[tagCursor.getCount()];
        while (tagCursor.moveToNext()) {
            Tag tag = tagCursor.getObject();
            tags[tagCursor.getPosition()] = tag.getName();
        }
        tagCursor.close();
		String[] topItems = { getResources().getString(R.string.notes), getResources().getString(R.string.trash) };
		mMenuItems = Arrays.copyOf(topItems, tags.length + 2);
        ActionBar ab = getActivity().getActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		System.arraycopy(tags, 0, mMenuItems, 2, tags.length);
		ArrayAdapter mSpinnerAdapter = new ArrayAdapter<String>(ab.getThemedContext(), android.R.layout.simple_spinner_item, mMenuItems);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ab.setListNavigationCallbacks(mSpinnerAdapter, this);
        if (mNavigationItem < mMenuItems.length) {
            ab.setSelectedNavigationItem(mNavigationItem);
        }
	}

	public void addNote() {
		// create & save new note
		Simplenote simplenote = (Simplenote) getActivity().getApplication();
		Bucket<Note> notesBucket = simplenote.getNotesBucket();
		Note note = notesBucket.newObject();
        note.setCreationDate(Calendar.getInstance());
		note.save(); 
		
		// refresh listview so new note appears
		refreshList();
		
		// nbradbury - call onNoteSelected() directly rather than using code below, since code below may not always select the correct note depending on user's sort preference
		mCallbacks.onNoteSelected(note);
	}

    public void setNoteSelected(Note selectedNote) {
        // Loop through notes and set note selected if found
        for(int i=0; i < mNotesAdapter.getCount(); i++) {
            Note note = (Note)mNotesAdapter.getItem(i);
            if (note.getSimperiumKey().equals(selectedNote.getSimperiumKey())) {
                setActivatedPosition(i);
                break;
            }
        }
    }

	public class NotesCursorAdapter extends CursorAdapter {
        private ObjectCursor<Note> mCursor;
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

            if (position == getListView().getCheckedItemPosition())
                view.setActivated(true);
            else
                view.setActivated(false);

            // for performance reasons we are going to get indexed values
            // from the cursor instead of instantiating the entire bucket object
            holder.contentTextView.setMaxLines(mNumPreviewLines);
            mCursor.moveToPosition(position);
            int pinned = mCursor.getInt(mCursor.getColumnIndex("pinned"));
            holder.pinImageView.setVisibility(pinned == 1 ? View.VISIBLE : View.GONE);

            String title = mCursor.getString(mCursor.getColumnIndex("title"));
            if (title == null) {
                title = getString(R.string.new_note);
            }
            holder.titleTextView.setText(title);

            if (mNumPreviewLines > 0) {
                holder.contentTextView.setText(mCursor.getString(mCursor.getColumnIndex("contentPreview")));
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
        private String mNoteId;

        public void setNoteId(String noteId) {
            mNoteId = noteId;
        }

        public String getNoteId() {
            return mNoteId;
        }
	}

	public void searchNotes(String searchString) {
        mSearchString = searchString;
		mNotesAdapter.changeCursor(queryNotes());
	}

    /**
     * Clear search and load all notes
     */
    public void clearSearch() {
        mSearchString = null;
        mNotesAdapter.changeCursor(queryNotes());
    }

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.d(Simplenote.TAG, "onNavigationItemSelected");

        if (!mNavListLoaded) {
            mNavListLoaded = true;
            return false;
        }

        // Update empty list placeholder text
        if (mEmptyListTextView != null) {
            if (itemPosition == NAVIGATION_ITEM_TRASH)
                mEmptyListTextView.setText(Html.fromHtml(getString(R.string.trash_is_empty)));
            else
                mEmptyListTextView.setText(Html.fromHtml(getString(R.string.no_notes)));
        }

        NotesActivity notesActivity = (NotesActivity)getActivity();
		mNavigationItem = itemPosition;
        if (itemPosition > 1) {
            mSelectedTag = mMenuItems[itemPosition];
        } else {
            mSelectedTag = null;
        }
        mNotesAdapter.changeCursor(queryNotes());

        notesActivity.invalidateOptionsMenu();
        if (notesActivity.isLargeScreenLandscape()) {
            if (mNotesAdapter.getCount() == 0) {
                notesActivity.showDetailPlaceholder();
            } else {
                // Select the first note
                Note firstNote = mNotesAdapter.getItem(0);
                mCallbacks.onNoteSelected(firstNote);
            }
        }
		
		return true;
	}

    public void sortNoteQuery(Query<Note> noteQuery){
        noteQuery.order("pinned", SortType.DESCENDING);
		int sortPref = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_SORT_ORDER);
		switch (sortPref) {
        case 0:
            noteQuery.order("modificationDate", SortType.DESCENDING);
            break;
		case 1:
            noteQuery.order("modificationDate", SortType.ASCENDING);
			break;
		case 2:
            noteQuery.order("creationDate", SortType.DESCENDING);
			break;
		case 3:
            noteQuery.order("creationDate", SortType.ASCENDING);
			break;
		case 4:
            noteQuery.order("content", SortType.ASCENDING);
			break;
		case 5:
            noteQuery.order("content", SortType.DESCENDING);
			break;
		}
    }


    private Bucket.Listener<Tag> mTagsMenuUpdater = new Bucket.Listener<Tag>(){
        void updateMenu(){
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    updateMenuItems();
                }
            });
        }

        public void onSaveObject(Bucket<Tag> bucket, Tag tag){
            updateMenu();
        }

        public void onDeleteObject(Bucket<Tag> bucket, Tag tag){
            updateMenu();
        }

        public void onChange(Bucket<Tag> bucket, Bucket.ChangeType type, String key){
            updateMenu();
        }
    };
}
