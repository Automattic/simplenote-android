package com.automattic.simplenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView.OnTagAddedListener;
import com.automattic.simplenote.utils.Typefaces;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;

import java.util.Calendar;

public class NoteEditorFragment extends Fragment implements TextWatcher, OnTagAddedListener, TextView.OnEditorActionListener {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_NEW_NOTE = "new_note";
    private static final int AUTOSAVE_DELAY_MILLIS = 2000;

    private Note mNote;
    private EditText mContentEditText;
    private TagsMultiAutoCompleteTextView mTagView;
    private ToggleButton mPinButton;
    private Handler mAutoSaveHandler;
    private LinearLayout mPlaceholderView;
    private CursorAdapter mAutocompleteAdapter;
    private boolean mIsNewNote;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteEditorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAutoSaveHandler = new Handler();

        mAutocompleteAdapter = new CursorAdapter(getActivity(), null, 0x0){

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                Activity activity = (Activity) context;
                if (activity == null) return null;
                return activity.getLayoutInflater().inflate(R.layout.tag_autocomplete_list_item, null);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView textView = (TextView) view;
                textView.setText(convertToString(cursor));
            }

            @Override
            public CharSequence convertToString(Cursor cursor){
                return cursor.getString(cursor.getColumnIndex(Tag.NAME_PROPERTY));
            }

            @Override
            public Cursor runQueryOnBackgroundThread(CharSequence filter){
                Log.v(Simplenote.TAG, String.format("runQueryOnBackgroundThread with filter: %s", filter));
                Activity activity = (Activity) getActivity();
                if (activity == null) return null;
                Simplenote application = (Simplenote) activity.getApplication();
                Query<Tag> query = application.getTagsBucket().query();
                // make the tag name available to the cursor
                query.include(Tag.NAME_PROPERTY);
                // sort the tags by their names
                query.order(Tag.NAME_PROPERTY);
                // if there's a filter string find only matching tag names
                if (filter != null ) query.where(Tag.NAME_PROPERTY, Query.ComparisonType.LIKE, String.format("%s%%", filter));
                return query.execute();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_note_editor, container, false);
        mContentEditText = ((EditText) rootView.findViewById(R.id.note_content));
        mContentEditText.setTypeface(Typefaces.get(getActivity().getBaseContext(), Simplenote.CUSTOM_FONT_PATH));
        mTagView = (TagsMultiAutoCompleteTextView) rootView.findViewById(R.id.tag_view);
        mTagView.setTokenizer(new SpaceTokenizer());
        mTagView.setTypeface(Typefaces.get(getActivity().getBaseContext(), Simplenote.CUSTOM_FONT_PATH));
        mTagView.setOnEditorActionListener(this);

        mPinButton = (ToggleButton) rootView.findViewById(R.id.pinButton);
        mPlaceholderView = (LinearLayout) rootView.findViewById(R.id.placeholder);
        if (((NotesActivity) getActivity()).isLargeScreenLandscape() && mNote == null)
            mPlaceholderView.setVisibility(View.VISIBLE);

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            String key = getArguments().getString(ARG_ITEM_ID);
            new loadNoteTask().execute(key);
            setIsNewNote(getArguments().getBoolean(ARG_NEW_NOTE, false));
        }

        mTagView.setAdapter(mAutocompleteAdapter);
		return rootView;
	}

    @Override
    public void onResume() {
        super.onResume();
        mTagView.setOnTagAddedListener(this);
    }

    @Override
    public void onPause() {
        Log.i("SIMPLENOTE", "EDITOR FRAGMENT PAUSED");

        // Delete the note if it is new and has empty fields
        if (mNote != null && mIsNewNote && noteIsEmpty())
            mNote.delete();
        else
            saveAndSyncNote();

        mTagView.setOnTagAddedListener(null);

        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);

        super.onPause();
    }

    private boolean noteIsEmpty() {
        return (mContentEditText.getText().toString().trim().length() == 0 && mTagView.getText().toString().trim().length() == 0);
    }

    public void setNote(String noteID) {
        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);

        mPlaceholderView.setVisibility(View.GONE);

        // If we have a note already (on a tablet in landscape), save the note.
        if (mNote != null) {
            if (mIsNewNote && noteIsEmpty())
                mNote.delete();
            else if (mNote != null)
                saveNote();
        }

        new loadNoteTask().execute(noteID);
    }

    public void refreshContent(boolean isNoteUpdate) {
        if (mNote != null) {
            Log.v("Simplenote", "refreshing content");
            // Restore the cursor position if possible.

            int cursorPosition = newCursorLocation(mNote.getContent(), mContentEditText.getText().toString(), mContentEditText.getSelectionEnd());
            mContentEditText.setText(mNote.getContent());
            if (isNoteUpdate && mContentEditText.hasFocus() && cursorPosition != mContentEditText.getSelectionEnd())
                mContentEditText.setSelection(cursorPosition);

            afterTextChanged(mContentEditText.getText());

            mPinButton.setChecked(mNote.isPinned());

            updateTagList();

            if (getActivity() != null)
                getActivity().invalidateOptionsMenu();
        }
    }

    public void updateTagList() {
        Activity activity = getActivity();
        if (activity == null) return;

        // Populate this note's tags in the tagView
        mTagView.setChips(mNote.getTagString());
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
    public void onTagsChanged(String tagString) {
        if (getActivity() != null && mNote.getTagString() != null && tagString.length() > mNote.getTagString().length())
            EasyTracker.getTracker().sendEvent("note", "added_tag", "tag_added_to_note", null);
        else
            EasyTracker.getTracker().sendEvent("note", "removed_tag", "tag_removed_from_note", null);

        mNote.setTagString(tagString);
        mNote.setModificationDate(Calendar.getInstance());
        updateTagList();
        mNote.save();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        // Unused
    }

    @Override
    public void afterTextChanged(Editable editable) {

        // Set the note title to be a larger size
        // Remove any existing size spans
        RelativeSizeSpan spans[] = editable.getSpans(0, editable.length(), RelativeSizeSpan.class);
        for (int i = 0; i < spans.length; i++) {
            editable.removeSpan(spans[i]);
        }
        int newLinePosition = mContentEditText.getText().toString().indexOf("\n");
        editable.setSpan(new RelativeSizeSpan(1.222f), 0, (newLinePosition > -1) ? newLinePosition : editable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

        // When text changes, start timer that will fire after AUTOSAVE_DELAY_MILLIS passes

        if (mAutoSaveHandler != null) {
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
            mAutoSaveHandler.postDelayed(autoSaveRunnable, AUTOSAVE_DELAY_MILLIS);
        }
    }

    private void saveAndSyncNote() {
        if (mNote == null)
            return;

        new saveNoteTask().execute();
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

    public void setIsNewNote(boolean isNewNote) {
        this.mIsNewNote = isNewNote;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            String tagString = mTagView.getText().toString().trim();
            if (tagString.length() > 0) {
                mTagView.setChips(tagString);
            }
        }
        return false;
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

    private class loadNoteTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            mContentEditText.removeTextChangedListener(NoteEditorFragment.this);
        }

        @Override
        protected Void doInBackground(String... args) {
            String noteID = args[0];
            NotesActivity notesActivity = (NotesActivity)getActivity();
            Simplenote application = (Simplenote) notesActivity.getApplication();
            Bucket<Note> notesBucket = application.getNotesBucket();
            try {
                mNote = notesBucket.get(noteID);
                notesActivity.setCurrentNote(mNote);
            } catch (BucketObjectMissingException e) {
                // TODO: Handle a missing note
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            refreshContent(false);
            mContentEditText.addTextChangedListener(NoteEditorFragment.this);
            if (mNote != null && mNote.getContent().isEmpty()) {
                // Show soft keyboard
                mContentEditText.requestFocus();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (inputMethodManager != null)
                            inputMethodManager.showSoftInput(mContentEditText, 0);
                    }
                }, 250);
            }
        }
    }

    private class saveNoteTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            saveNote();
            return null;
        }
    }

    private void saveNote() {
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
    }
}
