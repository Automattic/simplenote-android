package com.automattic.simplenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.MatchOffsetHighlighter;
import com.automattic.simplenote.utils.SimplenoteEditText;
import com.automattic.simplenote.utils.SimplenoteLinkify;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView.OnTagAddedListener;
import com.automattic.simplenote.utils.Typefaces;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;

import java.util.Calendar;

public class NoteEditorFragment extends Fragment implements TextWatcher, OnTagAddedListener, View.OnFocusChangeListener, SimplenoteEditText.OnSelectionChangedListener {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_NEW_NOTE = "new_note";
    static public final String ARG_MATCH_OFFSETS = "match_offsets";
    private static final int AUTOSAVE_DELAY_MILLIS = 2000;

    private Note mNote;
    private SimplenoteEditText mContentEditText;
    private TagsMultiAutoCompleteTextView mTagView;
    private ToggleButton mPinButton;
    private Handler mAutoSaveHandler;
    private LinearLayout mPlaceholderView;
    private CursorAdapter mAutocompleteAdapter;
    private boolean mIsNewNote;
    private ActionMode mActionMode;
    private MenuItem mViewLinkMenuItem;
    private String mLinkUrl;
    private String mLinkText;
    private MatchOffsetHighlighter mHighlighter;

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
            public Cursor runQueryOnBackgroundThread(CharSequence filter) {
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

    private MatchOffsetHighlighter.SpanFactory mMatchHighlighter = new MatchOffsetHighlighter.SpanFactory() {

        @Override
        public Object[] buildSpans(){
            return new Object[]{
                new ForegroundColorSpan(0xFFEEF3F8),
                new BackgroundColorSpan(0xFF4F91CC)
            };
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_note_editor, container, false);
        mContentEditText = ((SimplenoteEditText) rootView.findViewById(R.id.note_content));
        mContentEditText.setTypeface(Typefaces.get(getActivity().getBaseContext(), Simplenote.CUSTOM_FONT_PATH));
        mContentEditText.addOnSelectionChangedListener(this);
        mTagView = (TagsMultiAutoCompleteTextView) rootView.findViewById(R.id.tag_view);
        mTagView.setTokenizer(new SpaceTokenizer());
        mTagView.setTypeface(Typefaces.get(getActivity().getBaseContext(), Simplenote.CUSTOM_FONT_PATH));
        mTagView.setOnFocusChangeListener(this);

        mHighlighter = new MatchOffsetHighlighter(mMatchHighlighter, mContentEditText);

        mPinButton = (ToggleButton) rootView.findViewById(R.id.pinButton);
        mPinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPinButton.isChecked()) {
                    // Friendly message to the user as to what this button does.
                    Toast.makeText(getActivity(), R.string.note_pinned, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPlaceholderView = (LinearLayout) rootView.findViewById(R.id.placeholder);
        if ((getActivity() instanceof NotesActivity) && ((NotesActivity) getActivity()).isLargeScreenLandscape() && mNote == null)
            mPlaceholderView.setVisibility(View.VISIBLE);

        mTagView.setAdapter(mAutocompleteAdapter);
		return rootView;
	}

    @Override
    public void onResume() {
        super.onResume();
        mTagView.setOnTagAddedListener(this);

        Bundle arguments = getArguments();

        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            String key = arguments.getString(ARG_ITEM_ID);
            new loadNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
            setIsNewNote(getArguments().getBoolean(ARG_NEW_NOTE, false));
        }

    }

    @Override
    public void onPause() {
        // Hide soft keyboard if it is showing...
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null)
            inputMethodManager.hideSoftInputFromWindow(mContentEditText.getWindowToken(), 0);

        // Delete the note if it is new and has empty fields
        if (mNote != null && mIsNewNote && noteIsEmpty())
            mNote.delete();
        else
            saveAndSyncNote();

        mTagView.setOnTagAddedListener(null);

        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);

        mHighlighter.stop();
        super.onPause();
    }

    private boolean noteIsEmpty() {
        return (mContentEditText.getText().toString().trim().length() == 0 && mTagView.getText().toString().trim().length() == 0);
    }

    public void setNote(String noteID){
        setNote(noteID, null);
    }

    public void setNote(String noteID, String matchOffsets) {
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

        new loadNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteID, matchOffsets);
    }

    public void updateNote(Note updatedNote) {
        // update note if network change arrived
        mNote = updatedNote;
        refreshContent(true);
    }

    public void refreshContent(boolean isNoteUpdate) {
        if (mNote != null) {
            // Restore the cursor position if possible.

            int cursorPosition = newCursorLocation(mNote.getContent(), mContentEditText.getText().toString(), mContentEditText.getSelectionEnd());

            mContentEditText.setText(mNote.getContent());

            if (isNoteUpdate && mContentEditText.hasFocus() && cursorPosition != mContentEditText.getSelectionEnd())
                mContentEditText.setSelection(cursorPosition);

            afterTextChanged(mContentEditText.getText());

            mPinButton.setChecked(mNote.isPinned());

            updateTagList();
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
        if (mNote == null)
            return;
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
        if (newLinePosition == 0)
            return;
        editable.setSpan(new RelativeSizeSpan(1.222f), 0, (newLinePosition > 0) ? newLinePosition : editable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        SimplenoteLinkify.addLinks(mContentEditText, Linkify.ALL);

        int columnIndex = mNote.getBucket().getSchema().getFullTextIndex().getColumnIndex(Note.CONTENT_PROPERTY);
        String matches = getArguments().getString(ARG_MATCH_OFFSETS);
        mHighlighter.highlightMatches(matches, columnIndex);

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

        new saveNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            String tagString = mTagView.getText().toString().trim();
            if (tagString.length() > 0) {
                mTagView.setChips(tagString);
            }
        }
    }

    public Note getNote() {
        return mNote;
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
            Simplenote application = (Simplenote) getActivity().getApplication();
            Bucket<Note> notesBucket = application.getNotesBucket();
            try {
                mNote = notesBucket.get(noteID);
                // Set the current note in NotesActivity when on a tablet
                if (getActivity() instanceof NotesActivity) {
                    ((NotesActivity) getActivity()).setCurrentNote(mNote);
                }
            } catch (BucketObjectMissingException e) {
                // TODO: Handle a missing note
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            if (getActivity() == null || getActivity().isFinishing())
                return;
            refreshContent(true);
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
                }, 100);

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

    // Contextual action bar for dealing with links
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.view_link, menu);
            mViewLinkMenuItem = menu.findItem(R.id.menu_view_link);
            mode.setTitle(getString(R.string.link));
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                mode.setTitleOptionalHint(false);
            }
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_view_link:
                    if (mLinkUrl != null) {
                        try {
                            Uri uri = Uri.parse(mLinkUrl);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(uri);
                            startActivity(i);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mode.finish(); // Action picked, so close the CAB
                    }
                    return true;
                case R.id.menu_copy:
                    if (mLinkText != null) {
                        ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.app_name),mLinkText);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getActivity(), getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
                        mode.finish();
                    }
                    return true;
                case R.id.menu_share:
                    if (mLinkText != null) {
                        try {
                            Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mLinkText);
                            startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_note)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mode.finish();
                    }
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    // Checks if cursor is at a URL when the selection changes
    // If it is a URL, show the contextual action bar
    @Override
    public void onSelectionChanged(int selStart, int selEnd) {
        if (selStart == selEnd) {
            Editable noteContent = mContentEditText.getText();
            URLSpan[] urlSpans = noteContent.getSpans(selStart, selStart, URLSpan.class);
            if (urlSpans.length > 0) {
                URLSpan urlSpan = urlSpans[0];
                mLinkUrl = urlSpan.getURL();
                mLinkText = noteContent.subSequence(noteContent.getSpanStart(urlSpan), noteContent.getSpanEnd(urlSpan)).toString();
                if (mActionMode != null) {
                    mActionMode.setSubtitle(mLinkText);
                    setLinkMenuItem();
                    return;
                }

                // Show the Contextual Action Bar
                mActionMode = getActivity().startActionMode(mActionModeCallback);
                mActionMode.setSubtitle(mLinkText);
                setLinkMenuItem();
            } else if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }
        } else if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }

    private void setLinkMenuItem() {
        if (mViewLinkMenuItem != null && mLinkUrl != null) {
            if (mLinkUrl.startsWith("tel:")) {
                mViewLinkMenuItem.setIcon(R.drawable.ab_icon_call);
                mViewLinkMenuItem.setTitle(getString(R.string.call));
            } else if (mLinkUrl.startsWith("mailto:")) {
                mViewLinkMenuItem.setIcon(R.drawable.ab_icon_email);
                mViewLinkMenuItem.setTitle(getString(R.string.email));
            } else if (mLinkUrl.startsWith("geo:")) {
                mViewLinkMenuItem.setIcon(R.drawable.ab_icon_map);
                mViewLinkMenuItem.setTitle(getString(R.string.view_map));
            } else {
                mViewLinkMenuItem.setIcon(R.drawable.ab_icon_web);
                mViewLinkMenuItem.setTitle(getString(R.string.view_in_browser));
            }
        }
    }
}
