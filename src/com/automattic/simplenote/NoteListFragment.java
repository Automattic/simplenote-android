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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.client.Bucket;
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

	private NotesCursorAdapter mNotesAdapter;
	private Bucket<Note> mNotesBucket;
	private int mNumPreviewLines;
	private boolean mShowDate;
	private String[] mMenuItems;
	
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

		NoteDB db = application.getNoteDB();
		Cursor cursor = db.fetchAllNotes(getActivity().getBaseContext());
		String[] columns = new String[] { "content", "content", "creationDate" };
		int[] views = new int[] { R.id.note_title, R.id.note_content, R.id.note_date };
		mNotesAdapter = new NotesCursorAdapter(getActivity().getApplicationContext(), R.layout.note_list_row, cursor, columns, views, 0);
		setListAdapter(mNotesAdapter);
		
		mNotesBucket = application.getNotesBucket();

		getPrefs();

		mNotesBucket.addListener(mNotesAdapter);
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setDivider(getResources().getDrawable(R.drawable.list_divider));
        getListView().setDividerHeight(2);
        getListView().setBackgroundColor(getResources().getColor(R.color.white));
    }

    // nbradbury - load values from preferences
	protected void getPrefs() {
		mNumPreviewLines = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_NUM_PREVIEW_LINES, 2);
		mShowDate = PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_SHOW_DATES, true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
		}
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
		updateMenuItems();
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sCallbacks;
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
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	@SuppressWarnings("deprecation")
	public void refreshList() {
		Log.d(Simplenote.TAG, "Refresh the list");
		mNotesAdapter.getCursor().requery();
		mNotesAdapter.notifyDataSetChanged();
	}

	private void updateMenuItems() {
		// Update ActionBar menu
		Simplenote application = (Simplenote) getActivity().getApplication();
		String[] tags = application.getNoteDB().fetchAllTags();
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

	public class NotesCursorAdapter extends SimpleCursorAdapter implements Bucket.Listener<Note> {
		Context context;

		public NotesCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			this.context = context;
		}

        @Override
        public Object getItem(int position) {

            Cursor c = getCursor();
            c.moveToPosition(position);
            String noteID = c.getString(1);

            try {
                Note note = ((Simplenote) getActivity().getApplication()).getNotesBucket().get(noteID);
                return note;
            } catch (BucketObjectMissingException e) {
                e.printStackTrace();
            }

            return null;
        }

        /*
                 *  nbradbury - implemented "holder pattern" to boost performance with large note lists
                 */
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Log.d(Simplenote.TAG, String.format("Get view %d", position));

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

			// TODO: nbradbury - get rid of magic numbers for column indexes
			Cursor c = getCursor();
			c.moveToPosition(position);
            holder.setNoteId(c.getString(1));
			String title = c.getString(2);
			String content = c.getString(3);
			String contentPreview = c.getString(4);
			long modDateLong = c.getLong(6);
			boolean isPinned = (c.getInt(9) > 0);
			
			// nbradbury - changed so that pin is only shown if note is pinned - appears to the left of title (see note_list_row.xml)
			holder.pinImageView.setVisibility(isPinned ? View.VISIBLE : View.GONE);

			if (title != null) {
				holder.titleTextView.setText(title);
				holder.contentTextView.setText(contentPreview!=null ? contentPreview : content);
			} else {
				holder.titleTextView.setText(content);
				holder.contentTextView.setText(content);
			}

			holder.dateTextView.setVisibility(mShowDate ? View.VISIBLE : View.GONE);
			if (mShowDate) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(modDateLong * 1000);
				holder.dateTextView.setText(Note.dateString(cal, true, getActivity().getBaseContext()));
			}
			
			return view;
		}

		public void onObjectRemoved(String key, Note note) {
			Log.d(Simplenote.TAG, "Object removed, reload list view");
            ((NotesActivity)getActivity()).onNoteChanged(note);
			refreshUI();
		}

		public void onObjectUpdated(String key, Note note) {
			Log.d(Simplenote.TAG, "Object updated, reload list view");
            ((NotesActivity)getActivity()).onNoteChanged(note);
			refreshUI();
		}

		public void onObjectAdded(String key, Note note) {
			Log.d(Simplenote.TAG, "Object added, reload list view");
            ((NotesActivity)getActivity()).onNoteChanged(note);
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
		Simplenote application = (Simplenote) getActivity().getApplication();
		NoteDB db = application.getNoteDB();
		Cursor cursor = db.searchNotes(searchString);
		mNotesAdapter.changeCursor(cursor);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		Cursor cursor;
		Simplenote application = (Simplenote) getActivity().getApplication();
		NoteDB db = application.getNoteDB();
		
		// TODO: nbradbury - get rid of magic numbers here
		if (itemPosition == 0) {
			// All notes
			cursor = db.fetchAllNotes(getActivity().getBaseContext());
		} else if (itemPosition == 1) {
			// Trashed notes
			cursor = db.fetchDeletedNotes(getActivity().getBaseContext());
		} else {
			cursor = db.fetchNotesByTag(getActivity().getBaseContext(), mMenuItems[itemPosition]);
		}
		
		if (cursor != null)
			mNotesAdapter.changeCursor(cursor);

        getActivity().invalidateOptionsMenu();
		
		return true;
	}
}
