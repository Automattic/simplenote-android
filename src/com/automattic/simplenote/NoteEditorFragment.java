package com.automattic.simplenote;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;
import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;

/**
 * A fragment representing a single Note detail screen. This fragment is either
 * contained in a {@link NoteListActivity} in two-pane mode (on tablets) or a
 * {@link NoteEditorActivity} on handsets.
 */
public class NoteEditorFragment extends SherlockFragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	/**
	 * The dummy content this fragment is presenting.
	 */
	private Note mNote;
	private EditText mContentView;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteEditorFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
	        Application application = (Application)getActivity().getApplication();
			Bucket<Note> notesBucket = application.getNotesBucket();
			String key = getArguments().getString(ARG_ITEM_ID);
			mNote = (Note) notesBucket.get(key);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_note_editor, container, false);
		mContentView = ((EditText) rootView.findViewById(R.id.note_content));

		if (mNote != null) {
			mContentView.setText(mNote.getContent());
		}

		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		// Save here
	}
	
	
}
