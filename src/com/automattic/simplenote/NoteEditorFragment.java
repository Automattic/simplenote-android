package com.automattic.simplenote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

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
	private MultiAutoCompleteTextView mTagView;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteEditorFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO: nbradbury - what if ARG_ITEM_ID argument doesn't exist?
		if (getArguments().containsKey(ARG_ITEM_ID)) {
	        Simplenote application = (Simplenote)getActivity().getApplication();
			Bucket<Note> notesBucket = application.getNotesBucket();
			String key = getArguments().getString(ARG_ITEM_ID);			
			mNote = (Note) notesBucket.get(key);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_note_editor, container, false);
		mContentView = ((EditText) rootView.findViewById(R.id.note_content));
        mTagView = (MultiAutoCompleteTextView) rootView.findViewById(R.id.tag_view);

		if (mNote != null) {
			mContentView.setText(mNote.getContent());			
			if (mNote.getContent().isEmpty()) 
				mContentView.requestFocus();
			// Populate this note's tags in the tagView - TODO: nbradbury - for a large list of tags, using a StringBuilder here would be more efficient
	        String tagListString = "";
	        for (String tag : mNote.getTags()) 
	        	tagListString += tag + " ";
	        mTagView.setText(tagListString);
		}
		
		// Populate tag list
        Simplenote simplenote = (Simplenote)getActivity().getApplication();
		String[] allTags = simplenote.getNoteDB().fetchAllTags();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, allTags);
        mTagView.setAdapter(adapter);
        mTagView.setTokenizer(new SpaceTokenizer());
        
		return rootView;
	}
	
	@Override
	public void onPause() {
		String content = mContentView.getText().toString();
		String tagString = mTagView.getText().toString();
		List<String> tagList = Arrays.asList(tagString.split(" "));
		ArrayList<String> tags = new ArrayList<String>(tagList);
		
		// TODO: nbradbury - detect whether note has been modified, don't bother saving if unchanged
		// TODO: nbradbury - handle mNote==null
		// TODO: make sure any new tags get added to the Tag database
		mNote.setContent(content);
		mNote.setTags(tags);  
		mNote.save();
		
		super.onPause();
	}
	
	// Use spaces in tag autocompletion list
	// From http://stackoverflow.com/questions/3482981/how-to-replace-the-comma-with-a-space-when-i-use-the-multiautocompletetextview
	public class SpaceTokenizer implements Tokenizer {

		public int findTokenStart(CharSequence text, int cursor) {
			int i = cursor;
	
			while (i > 0 && text.charAt(i - 1) != ' ') {
			    i--;
			}
			while (i < cursor && text.charAt(i) == ' ') {
			    i++;
			}
	
			return i;
		}

		public int findTokenEnd(CharSequence text, int cursor) {
			int i = cursor;
			int len = text.length();
	
			while (i < len) {
			    if (text.charAt(i) == ' ') {
			        return i;
			    } else {
			        i++;
			    }
			}
	
			return len;
		}

		public CharSequence terminateToken(CharSequence text) {
			int i = text.length();
	
			while (i > 0 && text.charAt(i - 1) == ' ') {
			    i--;
			}
	
			if (i > 0 && text.charAt(i - 1) == ' ') {
			    return text;
			} else {
			    if (text instanceof Spanned) {
			        SpannableString sp = new SpannableString(text + " ");
			        TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
			                Object.class, sp, 0);
			        return sp;
			    } else {
			        return text + " ";
			    }
			}
		}
	}
}
