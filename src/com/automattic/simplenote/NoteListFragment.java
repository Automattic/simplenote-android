package com.automattic.simplenote;

import java.util.Arrays;
import java.util.Calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.OnSaveObjectListener;
import com.simperium.client.Bucket.OnDeleteObjectListener;
import com.simperium.client.Bucket.OnNetworkChangeListener;
import com.simperium.client.Query;
import com.simperium.client.Query.SortType;
import com.simperium.client.Query.ComparisonType;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.BucketObjectMissingException;

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
	private int mNumPreviewLines;
	private boolean mShowDate;
	private String[] mMenuItems;
    private String mSelectedTag;
    private String mSearchString;
	private int mNavigationItem;
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
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

        // Cursor cursor = db.fetchAllNotes(getActivity().getBaseContext());
        ObjectCursor<Note> cursor = queryNotes();
		mNotesAdapter = new NotesCursorAdapter(getActivity().getBaseContext(), cursor, 0);
		setListAdapter(mNotesAdapter);
		

		getPrefs();

		mNotesBucket.registerOnSaveObjectListener(mNotesAdapter);
		mNotesBucket.registerOnDeleteObjectListener(mNotesAdapter);
		mNotesBucket.registerOnNetworkChangeListener(mNotesAdapter);
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    // nbradbury - load values from preferences
	protected void getPrefs() {
		mNumPreviewLines = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_NUM_PREVIEW_LINES, 2);
		mShowDate = PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_SHOW_DATES, true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        setListShown(true);

        TextView emptyListTextView = (TextView)getActivity().getLayoutInflater().inflate(R.layout.empty_list, null);
        getListView().setEmptyView(emptyListTextView);
        emptyListTextView.setVisibility(View.GONE);
        ((ViewGroup)getListView().getParent()).addView(emptyListTextView);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
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
        refreshList();
        // update the view again
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
		mNotesBucket.unregisterOnSaveObjectListener(mNotesAdapter);
		mNotesBucket.unregisterOnDeleteObjectListener(mNotesAdapter);
		mNotesBucket.registerOnNetworkChangeListener(mNotesAdapter);
    }

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		Note note = (Note)getListAdapter().getItem(position);
        if (note != null)
            mCallbacks.onNoteSelected(note);
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

		// Also select the first item by default
		// TODO: persist the last selected item and restore that instead
		if (!mNotesAdapter.isEmpty()) {
			View v = mNotesAdapter.getView(0, null, null);
			getListView().performItemClick(v, 0, 0);
		}
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
        sortNoteQuery(query);
        return query.execute();
    }

	private void updateMenuItems() {
		// Update ActionBar menu
		Simplenote application = (Simplenote) getActivity().getApplication();
        Bucket<Tag> tagBucket = application.getTagsBucket();
        ObjectCursor<Tag> tagCursor = tagBucket.query().orderByKey().execute();
		String[] tags = new String[tagCursor.getCount()];
        while (tagCursor.moveToNext()) {
            tags[tagCursor.getPosition()] = tagCursor.getObject().getName();
        }
        tagCursor.close();
		String[] topItems = { getResources().getString(R.string.notes), getResources().getString(R.string.trash) };
		mMenuItems = Arrays.copyOf(topItems, tags.length + 2);
        ActionBar ab = getActivity().getActionBar();
		System.arraycopy(tags, 0, mMenuItems, 2, tags.length);
		ArrayAdapter mSpinnerAdapter = new ArrayAdapter<String>(ab.getThemedContext(), android.R.layout.simple_spinner_item, mMenuItems);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ab.setListNavigationCallbacks(mSpinnerAdapter, this);
	}

	public void addNote() {
		// TODO: nbradbury - creating & saving a new note here causes an empty note to appear which doesn't go away if user backs out
		// of editing the note (iOS app suffers from the same problem) - instead NoteEditorFragment should be changed to handle new
		// note creation and only save the note if user has entered any content
		
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
		
		// Select the new note
		//View v = mNotesAdapter.getView(0, null, null);
		//getListView().performItemClick(v, 0, 0);
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

	public class NotesCursorAdapter extends CursorAdapter
    implements OnSaveObjectListener<Note>,
    OnDeleteObjectListener<Note>,
    OnNetworkChangeListener {
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
				holder.dateTextView = (TextView) view.findViewById(R.id.note_date);
				holder.pinImageView = (ImageView) view.findViewById(R.id.note_pin);
				view.setTag(holder);
			} else {
				holder = (NoteViewHolder) view.getTag();
			}

			holder.contentTextView.setMaxLines(mNumPreviewLines);

            Note note = getItem(position);

            if (note != null) {
                String title = note.getTitle();
                String content = note.getContent().trim();
                String contentPreview = note.getContentPreview();
                // nbradbury - changed so that pin is only shown if note is pinned - appears to the left of title (see note_list_row.xml)
                holder.pinImageView.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

                if (title != null) {
                    title = title.trim();
                    holder.titleTextView.setText(title);
                    holder.contentTextView.setText(contentPreview!=null ? contentPreview.trim() : content);
                } else {
                    holder.titleTextView.setText(content.equals("") ? getString(R.string.new_note) : content);
                    holder.contentTextView.setText(content);
                }

                holder.dateTextView.setVisibility(mShowDate ? View.VISIBLE : View.GONE);
                if (mShowDate) {
                    holder.dateTextView.setText(Note.dateString(note.getModificationDate(), true, getActivity().getBaseContext()));
                }
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

        @Override
        public void onDeleteObject(Note note) {
			Log.d(Simplenote.TAG, "Object removed, reload list view");
            ((NotesActivity)getActivity()).onNoteChanged(note);
			refreshUI();
		}

        @Override
		public void onSaveObject(Note note) {
			Log.d(Simplenote.TAG, "Object added, reload list view");
            ((NotesActivity)getActivity()).onNoteChanged(note);
			refreshUI();
		}

        @Override
        public void onChange(Bucket.ChangeType type, String key){
			Log.d(Simplenote.TAG, "Network change, reload list view");
			refreshUI();
        }

		private void refreshUI() {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					refreshList();
				}
			});
		}
	}
	
	// view holder for NotesCursorAdapter
	private static class NoteViewHolder {
		TextView titleTextView;
		TextView contentTextView;
		TextView dateTextView;
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

		Query<Note> query;
		Simplenote application = (Simplenote) getActivity().getApplication();
        Bucket<Note> noteBucket = application.getNotesBucket();
		mNavigationItem = itemPosition;
        if (itemPosition > 1) {
            mSelectedTag = mMenuItems[itemPosition];
        } else {
            mSelectedTag = null;
        }
        mNotesAdapter.changeCursor(queryNotes());

        getActivity().invalidateOptionsMenu();
		
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
            // orderBy = "creationDate DESC";
            noteQuery.order("creationDate", SortType.DESCENDING);
			break;
		case 2:
            noteQuery.order("content", SortType.ASCENDING);
			break;
		case 3:
            noteQuery.order("modificationDate", SortType.ASCENDING);
			break;
		case 4:
            noteQuery.order("creationDate", SortType.ASCENDING);
			break;
		case 5:
            noteQuery.order("content", SortType.DESCENDING);
			break;
		}
    }
}
