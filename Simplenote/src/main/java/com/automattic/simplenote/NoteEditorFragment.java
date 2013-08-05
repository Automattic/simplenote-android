package com.automattic.simplenote;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.ToggleButton;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView.OnTagAddedListener;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.BucketObjectMissingException;

public class NoteEditorFragment extends Fragment implements TextWatcher, OnTagAddedListener {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    private static final int AUTOSAVE_DELAY_MILLIS = 2000;

    /**
     * The dummy content this fragment is presenting.
     */
    private Note mNote;
    private EditText mContentEditText;
    private TagsMultiAutoCompleteTextView mTagView;
    private ToggleButton mPinButton;
    private boolean mShowNoteTitle;
    private Handler mAutoSaveHandler;
    private LinearLayout mPlaceholderView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteEditorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            Simplenote application = (Simplenote) getActivity().getApplication();
            Bucket<Note> notesBucket = application.getNotesBucket();
            String key = getArguments().getString(ARG_ITEM_ID);
            try {
                mNote = notesBucket.get(key);
            } catch (BucketObjectMissingException e) {
                // TODO: Handle a missing note
            }
        }

        if (!((NotesActivity) getActivity()).isLargeScreenLandscape())
            mShowNoteTitle = true;

        mAutoSaveHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_note_editor, container, false);
        mContentEditText = ((EditText) rootView.findViewById(R.id.note_content));
        mContentEditText.addTextChangedListener(this);
        mTagView = (TagsMultiAutoCompleteTextView) rootView.findViewById(R.id.tag_view);
        mTagView.setTokenizer(new SpaceTokenizer());

        mPinButton = (ToggleButton) rootView.findViewById(R.id.pinButton);
        mPlaceholderView = (LinearLayout) rootView.findViewById(R.id.placeholder);
        if (((NotesActivity) getActivity()).isLargeScreenLandscape() && mNote == null)
            mPlaceholderView.setVisibility(View.VISIBLE);

		refreshContent();

		return rootView;
	}

    @Override
    public void onResume() {
        super.onResume();
        mTagView.setOnTagAddedListener(this);
        if (mNote != null && mNote.getContent().isEmpty()) {
            // Show soft keyboard
            mContentEditText.requestFocus();

            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null)
                inputMethodManager.showSoftInput(mContentEditText, 0);
        }
    }

    @Override
    public void onPause() {
        saveAndSyncNote();
        mTagView.setOnTagAddedListener(null);
        // Hide soft keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null)
            inputMethodManager.hideSoftInputFromWindow(mContentEditText.getWindowToken(), 0);

        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);

        super.onPause();
    }

    public void setNote(Note note) {
        mPlaceholderView.setVisibility(View.GONE);

        // If we have a note already (on a tablet in landscape), save the note.
        if (mNote != null)
            saveAndSyncNote();

        mNote = note;
        refreshContent();
    }

    public void refreshContent() {
        if (mNote != null) {
            Log.v("Simplenote", "refreshing content");
            // Restore the cursor position if possible.
            int cursorPosition = newCursorLocation(mNote.getContent(), mContentEditText.getText().toString(), mContentEditText.getSelectionEnd());
            mContentEditText.setText(mNote.getContent());
            if (mContentEditText.hasFocus() && cursorPosition != mContentEditText.getSelectionEnd())
                mContentEditText.setSelection(cursorPosition);

            mPinButton.setChecked(mNote.isPinned());

            setActionBarTitle();

            updateTagList();
        }
    }

    public void updateTagList(){
        // Populate this note's tags in the tagView
        mTagView.setChips(mNote.getTagString());
        
		// Populate tag list
        Simplenote simplenote = (Simplenote)getActivity().getApplication();
        Bucket<Tag> tagBucket = simplenote.getTagsBucket();
        ObjectCursor<Tag> tagsCursor = tagBucket.query().orderByKey().execute();
        List<String> allTags = new ArrayList<String>(tagsCursor.getCount());
        while (tagsCursor.moveToNext()) {
            Tag tag = tagsCursor.getObject();
            if (!mNote.hasTag(tag)) allTags.add(tag.getName());
        }
        tagsCursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, allTags);
        mTagView.setAdapter(adapter);
    }

    private void setActionBarTitle() {
        if (mShowNoteTitle) {
            if (mNote.getTitle() != null && !mNote.getTitle().equals(""))
                getActivity().getActionBar().setTitle(mNote.getTitle());
            else
                getActivity().getActionBar().setTitle(R.string.new_note);
        }
    }

    int newCursorLocation(String newText, String oldText, int cursorLocation) {
        // Ported from the iOS app :)
        // Cases:
        // 0. All text after cursor (and possibly more) was removed ==> put cursor at end
        // 1. Text was added after the cursor ==> no change
        // 2. Text was added before the cursor ==> location advances
        // 3. Text was removed after the cursor ==> no change
        // 4. Text was removed before the cursor ==> location retreats
        // 5. Text was added/removed on both sides of the cursor ==> not handled

        int newCursorLocation = cursorLocation;

        int deltaLength = newText.length() - oldText.length();

        // Case 0
        if (newText.length() < cursorLocation)
            return newText.length();

        boolean beforeCursorMatches = false;
        boolean afterCursorMatches = false;

        try {
            beforeCursorMatches = oldText.substring(0, cursorLocation).equals(newText.substring(0, cursorLocation));
            afterCursorMatches = oldText.substring(cursorLocation, oldText.length()).equals(newText.substring(cursorLocation + deltaLength, newText.length()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Cases 2 and 4
        if (!beforeCursorMatches && afterCursorMatches)
            newCursorLocation += deltaLength;

        // Cases 1, 3 and 5 have no change
        return newCursorLocation;
    }

    private Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveAndSyncNote();
        }
    };

    @Override
    public void onTagsChanged(String tagString){
        mNote.setTagString(tagString);
        mNote.setModificationDate(Calendar.getInstance());
        updateTagList();
        mNote.save();
        if (getActivity() != null)
            EasyTracker.getTracker().sendEvent("note", "added_tag", "tag_added_to_note", null);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        // Unused
    }

    @Override
    public void afterTextChanged(Editable editable) {
        // Unused

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        // When text changes, start timer that will fire after AUTOSAVE_DELAY_MILLIS passes

        if (mAutoSaveHandler != null) {
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
            mAutoSaveHandler.postDelayed(autoSaveRunnable, AUTOSAVE_DELAY_MILLIS);
        }
    }

    private void saveAndSyncNote() {
        if (mNote == null)
            return;

        String content = mContentEditText.getText().toString();
        String tagString = mTagView.getText().toString();
        if (mNote.hasChanges(content, tagString.trim(), mPinButton.isChecked())) {
            mNote.setContent(content);
            mNote.setTagString(mTagView.getText().toString());
            mNote.setModificationDate(Calendar.getInstance());
            // Send pinned event to google analytics if changed
            mNote.setPinned(mPinButton.isChecked());
            mNote.save();
            if (getActivity() != null) {
                Tracker tracker = EasyTracker.getTracker();
                if (mNote.isPinned() != mPinButton.isChecked())
                    tracker.sendEvent("note", (mPinButton.isChecked()) ? "pinned_note" : "unpinned_note", "pin_button", null);
                tracker.sendEvent("note", "edited_note", "editor_save", null);
            }
        }

        setActionBarTitle();

        Log.v("Simplenote", "autosaving note");
    }

    public void setPlaceholderVisible(boolean isVisible) {
        if (isVisible) {
            mNote = null;
            mContentEditText.setText("");
            mTagView.setText("");
            if (mPlaceholderView != null)
                mPlaceholderView.setVisibility(View.VISIBLE);
        } else {
            if (mPlaceholderView != null)
                mPlaceholderView.setVisibility(View.GONE);
        }
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
