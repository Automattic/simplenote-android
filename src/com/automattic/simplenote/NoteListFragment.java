package com.automattic.simplenote;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;

import com.simperium.client.Bucket;

import android.util.Log;

/**
 * A list fragment representing a list of Notes. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link NoteEditorFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends SherlockListFragment {

	private NotesCursorAdapter mNotesAdapter;
	private Bucket<Note> mNotesBucket;
	private int mNumPreviewLines;
	private boolean mShowDate;

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
		Cursor cursor = db.fetchAllNotes(getActivity());
		mNotesBucket = application.getNotesBucket();
		
		String[] columns = new String[] { "content", "content", "creationDate" };
		int[] views = new int[] { R.id.note_title, R.id.note_content, R.id.note_date };

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mNumPreviewLines = Integer.valueOf(sharedPref.getString("pref_key_preview_lines", "2"));
		mShowDate = sharedPref.getBoolean("pref_key_show_dates", true);

		mNotesAdapter = new NotesCursorAdapter(getActivity().getApplicationContext(), R.layout.note_list_row, cursor, columns, views, 0);
		mNotesBucket.addListener(mNotesAdapter);

		setListAdapter(mNotesAdapter);
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
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		
		// Move cursor to this row
		Simplenote simplenote = (Simplenote) getActivity().getApplication();
		NoteDB db = simplenote.getNoteDB();
		Cursor cursor = db.fetchAllNotes(getActivity());
		cursor.moveToPosition(position);
		
		// Get the simperiumKey and retrieve the note via Simperium
		String simperiumKey = cursor.getString(1);
		Bucket<Note> notesBucket = simplenote.getNotesBucket();
		Note note = notesBucket.get(simperiumKey);
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
		mNotesAdapter.c.requery();
		mNotesAdapter.notifyDataSetChanged();
	}
	
	public void addNote() {
		Simplenote simplenote = (Simplenote) getActivity().getApplication();
		Bucket<Note> notesBucket = simplenote.getNotesBucket();
		notesBucket.newObject();
		refreshList();
		
		// Select the new note
		View v = mNotesAdapter.getView(0, null, null);
		getListView().performItemClick(v, 0, 0);
	}

	public class NotesCursorAdapter extends SimpleCursorAdapter implements Bucket.Listener<Note> {
		
		Cursor c;
		Context context;
		LinearLayout.LayoutParams lp;

		public NotesCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			this.c = c;
			this.context = context;
			lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null)
				view = View.inflate(getActivity().getBaseContext(), R.layout.note_list_row, null);

			c.moveToPosition(position);

			TextView titleTextView = (TextView) view.findViewById(R.id.note_title);
			TextView contentTextView = (TextView) view.findViewById(R.id.note_content);
			TextView dateTextView = (TextView) view.findViewById(R.id.note_date);
			ImageView pinImageView = (ImageView) view.findViewById(R.id.note_pin);

			contentTextView.setMaxLines(mNumPreviewLines);

			if (mShowDate) {
				dateTextView.setVisibility(View.VISIBLE);
				lp.setMargins(0, 0, Math.round(60 * context.getResources().getDisplayMetrics().density), 0);
				titleTextView.setLayoutParams(lp);
			} else {
				dateTextView.setVisibility(View.GONE);
				lp.setMargins(0, 0, 0, 0);
				titleTextView.setLayoutParams(lp);
			}

			if (c.getInt(10) > 0) {
				pinImageView.setImageResource(R.drawable.ic_item_list_default_pin_active);
			} else {
				pinImageView.setImageResource(R.drawable.ic_item_list_default_pin);
			}

			String title = c.getString(2);
			if (title != null) {
				titleTextView.setText(c.getString(2));
				if (c.getString(4) != null)
					contentTextView.setText(c.getString(4));
				else
					contentTextView.setText(c.getString(3));
			} else {
				titleTextView.setText(c.getString(3));
				contentTextView.setText(c.getString(3));
			}

			String formattedDate = android.text.format.DateFormat.getTimeFormat(context).format(new Date(c.getLong(6)));

			dateTextView.setText(formattedDate);

			return view;
		}
		
		public void onObjectRemoved(String key, Note object){
			refreshUI();
		}
		public void onObjectUpdated(String key, Note object){
			refreshUI();
		}
		public void onObjectAdded(String key, Note object){
			Log.d("Simplenote", "Object added, reload list view");
			refreshUI();
		}
		
		private void refreshUI(){
			getActivity().runOnUiThread(new Runnable(){
				public void run(){
					refreshList();
				}
			});
		}
	}

	public void searchNotes(String searchString) {
		NoteDB db = new NoteDB(getActivity().getApplicationContext());
		Cursor cursor = db.searchNotes(searchString);
		mNotesAdapter.swapCursor(cursor);
		mNotesAdapter.notifyDataSetChanged();
		
	}
}
