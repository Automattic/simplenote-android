package com.automattic.simplenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.AutoBullet;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.MatchOffsetHighlighter;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ShareButtonAdapter;
import com.automattic.simplenote.utils.SimplenoteLinkify;
import com.automattic.simplenote.utils.SpaceTokenizer;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView.OnTagAddedListener;
import com.automattic.simplenote.utils.TextHighlighter;
import com.automattic.simplenote.widgets.SimplenoteEditText;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class NoteEditorFragment extends Fragment implements Bucket.Listener<Note>, TextWatcher, OnTagAddedListener, View.OnFocusChangeListener, SimplenoteEditText.OnSelectionChangedListener {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_NEW_NOTE = "new_note";
    static public final String ARG_MATCH_OFFSETS = "match_offsets";
    private static final int AUTOSAVE_DELAY_MILLIS = 2000;
    private static final int MAX_REVISIONS = 30;

    private Note mNote;
    private Bucket<Note> mNotesBucket;

    private View mRootView;
    private SimplenoteEditText mContentEditText;
    private TagsMultiAutoCompleteTextView mTagView;
    private View mHistoryView;
    private SeekBar mHistorySeekBar;
    private TextView mHistoryDate;

    private ToggleButton mPinButton;

    private Handler mAutoSaveHandler;
    private Handler mPublishTimeoutHandler;

    private LinearLayout mPlaceholderView;
    private CursorAdapter mAutocompleteAdapter;
    private boolean mIsNewNote, mIsLoadingNote, mDidTapHistoryButton;
    private ActionMode mActionMode;
    private MenuItem mViewLinkMenuItem;
    private String mLinkUrl;
    private String mLinkText;
    private MatchOffsetHighlighter mHighlighter;
    private int mEmailIconResId, mWebIconResId, mMapIconResId, mCallIconResId;
    private MatchOffsetHighlighter.SpanFactory mMatchHighlighter;
    private String mMatchOffsets;
    private int mCurrentCursorPosition;
    private ArrayList<Note> mNoteRevisionsList;

    private BottomSheetDialog mHistoryBottomSheet;
    private BottomSheetDialog mInfoBottomSheet;
    private BottomSheetDialog mShareBottomSheet;

    private Snackbar mPublishingSnackbar;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteEditorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() != null) {
            Simplenote currentApp = (Simplenote) getActivity().getApplication();
            mNotesBucket = currentApp.getNotesBucket();

            TypedArray a = getActivity().obtainStyledAttributes(new int[]{R.attr.actionBarIconEmail, R.attr.actionBarIconWeb, R.attr.actionBarIconMap, R.attr.actionBarIconCall});
            if (a != null) {
                mEmailIconResId = a.getResourceId(0, 0);
                mWebIconResId = a.getResourceId(1, 0);
                mMapIconResId = a.getResourceId(2, 0);
                mCallIconResId = a.getResourceId(3, 0);
                a.recycle();
            }
        }

        mAutoSaveHandler = new Handler();
        mPublishTimeoutHandler = new Handler();

        mMatchHighlighter = new TextHighlighter(getActivity(),
                R.attr.editorSearchHighlightForegroundColor, R.attr.editorSearchHighlightBackgroundColor);
        mAutocompleteAdapter = new CursorAdapter(getActivity(), null, 0x0) {
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
                Activity activity = getActivity();
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
        setHasOptionsMenu(true);
        mRootView = inflater.inflate(R.layout.fragment_note_editor, container, false);
        mContentEditText = ((SimplenoteEditText) mRootView.findViewById(R.id.note_content));
        mContentEditText.addOnSelectionChangedListener(this);
        mTagView = (TagsMultiAutoCompleteTextView) mRootView.findViewById(R.id.tag_view);
        mTagView.setTokenizer(new SpaceTokenizer());
        mTagView.setOnFocusChangeListener(this);

        mHighlighter = new MatchOffsetHighlighter(mMatchHighlighter, mContentEditText);

        mPinButton = (ToggleButton) mRootView.findViewById(R.id.pinButton);

        mPinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPinButton.isChecked()) {
                    // Friendly message to the user as to what this button does.
                    Toast.makeText(getActivity(), R.string.note_pinned, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPlaceholderView = (LinearLayout) mRootView.findViewById(R.id.placeholder);
        if (DisplayUtils.isLargeScreenLandscape(getActivity()) && mNote == null) {
            mPlaceholderView.setVisibility(View.VISIBLE);
        }

        mTagView.setAdapter(mAutocompleteAdapter);

        // Load note if we were passed a note Id
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            String key = arguments.getString(ARG_ITEM_ID);
            if (arguments.containsKey(ARG_MATCH_OFFSETS)) {
                mMatchOffsets = arguments.getString(ARG_MATCH_OFFSETS);
            }
            new loadNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
            setIsNewNote(getArguments().getBoolean(ARG_NEW_NOTE, false));
        }

		return mRootView;
	}

    @Override
    public void onResume() {
        super.onResume();
        mNotesBucket.start();
        mNotesBucket.addListener(this);

        mTagView.setOnTagAddedListener(this);

        if (mContentEditText != null) {
            mContentEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_FONT_SIZE, 18));
        }
    }

    @Override
    public void onPause() {
        mNotesBucket.removeListener(this);
        // Hide soft keyboard if it is showing...
        if (getActivity() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(mContentEditText.getWindowToken(), 0);
            }
        }

        // Delete the note if it is new and has empty fields
        if (mNote != null && mIsNewNote && noteIsEmpty()) {
            mNote.delete();
        } else {
            saveNote();
        }

        mTagView.setOnTagAddedListener(null);

        if (mAutoSaveHandler != null) {
            mAutoSaveHandler.removeCallbacks(mAutoSaveRunnable);
        }

        if (mPublishTimeoutHandler != null) {
            mPublishTimeoutHandler.removeCallbacks(mPublishTimeoutRunnable);
        }

        mHighlighter.stop();

        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded() || DisplayUtils.isLargeScreenLandscape(getActivity())) {
            return;
        }

        inflater.inflate(R.menu.note_editor, menu);

        if (mNote != null) {
            MenuItem viewPublishedNoteItem = menu.findItem(R.id.menu_view_info);
            viewPublishedNoteItem.setVisible(true);

            MenuItem trashItem = menu.findItem(R.id.menu_delete).setTitle(R.string.undelete);
            if (mNote.isDeleted())
                trashItem.setTitle(R.string.undelete);
            else
                trashItem.setTitle(R.string.delete);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_info:
                if (mNote != null) {
                    showInfoSheet();
                }
                return true;

            case R.id.menu_history:
                if (mNote != null && mNote.getVersion() > 1) {
                    showHistory();
                }
                return true;
            case R.id.menu_share:
                showShareSheet();
                return true;
            case R.id.menu_delete:
                if (!isAdded()) return false;

                if (mNote != null) {
                    mNote.setDeleted(!mNote.isDeleted());
                    mNote.setModificationDate(Calendar.getInstance());
                    mNote.save();
                    Intent resultIntent = new Intent();
                    if (mNote.isDeleted()) {
                        resultIntent.putExtra(Simplenote.DELETED_NOTE_ID, mNote.getSimperiumKey());
                    }
                    getActivity().setResult(Activity.RESULT_OK, resultIntent);

                    AnalyticsTracker.track(
                            AnalyticsTracker.Stat.EDITOR_NOTE_DELETED,
                            AnalyticsTracker.CATEGORY_NOTE,
                            "trash_menu_item"
                    );
                }

                getActivity().finish();
                return true;
            case android.R.id.home:
                if (!isAdded()) return false;

                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareNote() {
        if (mNote != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mNote.getContent());
            startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_note)));
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_NOTE_CONTENT_SHARED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "action_bar_share_button"
            );
        }
    }

    private void showHistory() {
        // Request revisions for the current note
        mNotesBucket.getRevisions(mNote, MAX_REVISIONS, mRevisionsRequestCallbacks);
        saveNote();
        showHistorySheet();
    }

    private boolean noteIsEmpty() {
        return (getNoteContentString().trim().length() == 0 && getNoteTagsString().trim().length() == 0);
    }

    public void setNote(String noteID){
        setNote(noteID, null);
    }

    public void setNote(String noteID, String matchOffsets) {
        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(mAutoSaveRunnable);

        mPlaceholderView.setVisibility(View.GONE);

        if (matchOffsets != null) {
            mMatchOffsets = matchOffsets;
        } else {
            mMatchOffsets = null;
        }

        // If we have a note already (on a tablet in landscape), save the note.
        if (mNote != null) {
            if (mIsNewNote && noteIsEmpty())
                mNote.delete();
            else if (mNote != null)
                saveNote();
        }

        new loadNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteID);
    }

    private void updateNote(Note updatedNote) {
        // update note if network change arrived
        mNote = updatedNote;
        refreshContent(true);
    }

    private void refreshContent(boolean isNoteUpdate) {
        if (mNote != null) {
            // Restore the cursor position if possible.

            int cursorPosition = newCursorLocation(mNote.getContent(), getNoteContentString(), mContentEditText.getSelectionEnd());

            mContentEditText.setText(mNote.getContent());

            if (isNoteUpdate) {
                // Save the note so any local changes get synced
                mNote.save();

                if (mContentEditText.hasFocus() && cursorPosition != mContentEditText.getSelectionEnd()) {
                    mContentEditText.setSelection(cursorPosition);
                }
            }

            afterTextChanged(mContentEditText.getText());

            mPinButton.setChecked(mNote.isPinned());

            updateTagList();
        }
    }

    private void updateTagList() {
        Activity activity = getActivity();
        if (activity == null) return;

        // Populate this note's tags in the tagView
        mTagView.setChips(mNote.getTagString());
    }

    private int newCursorLocation(String newText, String oldText, int cursorLocation) {
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

    private final Runnable mAutoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveAndSyncNote();
        }
    };

    @Override
    public void onTagsChanged(String tagString) {
        if (mNote == null || !isAdded()) return;

        if (mNote.getTagString() != null && tagString.length() > mNote.getTagString().length()) {
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_TAG_ADDED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "tag_added_to_note"
            );
        }
        else {
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_TAG_REMOVED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "tag_removed_from_note"
            );
        }

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
        setTitleSpan(editable);
        attemptAutoList(editable);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

        // When text changes, start timer that will fire after AUTOSAVE_DELAY_MILLIS passes
        if (mAutoSaveHandler != null) {
            mAutoSaveHandler.removeCallbacks(mAutoSaveRunnable);
            mAutoSaveHandler.postDelayed(mAutoSaveRunnable, AUTOSAVE_DELAY_MILLIS);
        }

        // Remove search highlight spans when note content changes
        if (mMatchOffsets != null) {
            mMatchOffsets = null;
            mHighlighter.removeMatches();
        }
    }

    private void setTitleSpan(Editable editable) {
        // Set the note title to be a larger size
        // Remove any existing size spans
        RelativeSizeSpan spans[] = editable.getSpans(0, editable.length(), RelativeSizeSpan.class);
        for (RelativeSizeSpan span : spans) {
            editable.removeSpan(span);
        }
        int newLinePosition = getNoteContentString().indexOf("\n");
        if (newLinePosition == 0)
            return;
        editable.setSpan(new RelativeSizeSpan(1.227f), 0, (newLinePosition > 0) ? newLinePosition : editable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    private void attemptAutoList(Editable editable) {
        int oldCursorPosition = mCurrentCursorPosition;
        mCurrentCursorPosition = mContentEditText.getSelectionStart();
        AutoBullet.apply(editable, oldCursorPosition, mCurrentCursorPosition);
        mCurrentCursorPosition = mContentEditText.getSelectionStart();
    }

    private void saveAndSyncNote() {
        if (mNote == null) {
            return;
        }

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
            String tagString = getNoteTagsString().trim();
            if (tagString.length() > 0) {
                mTagView.setChips(tagString);
            }
        }
    }

    private Note getNote() {
        return mNote;
    }

    private String getNoteContentString() {
        if (mContentEditText == null || mContentEditText.getText() == null) {
            return "";
        } else {
            return mContentEditText.getText().toString();
        }
    }

    private String getNoteTagsString() {
        if (mTagView == null || mTagView.getText() == null) {
            return "";
        } else {
            return mTagView.getText().toString();
        }
    }

    private class loadNoteTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            mContentEditText.removeTextChangedListener(NoteEditorFragment.this);
            mIsLoadingNote = true;
        }

        @Override
        protected Void doInBackground(String... args) {
            if (getActivity() == null) {
                return null;
            }

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
            refreshContent(false);
            if (mMatchOffsets != null) {
                int columnIndex = mNote.getBucket().getSchema().getFullTextIndex().getColumnIndex(Note.CONTENT_PROPERTY);
                mHighlighter.highlightMatches(mMatchOffsets, columnIndex);
            }
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

            SimplenoteLinkify.addLinks(mContentEditText, Linkify.ALL);

            mIsLoadingNote = false;
        }
    }

    private class saveNoteTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            saveNote();
            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                // Update links
                SimplenoteLinkify.addLinks(mContentEditText, Linkify.ALL);
            }
        }
    }

    private void saveNote() {
        if (mNote == null || (mHistoryBottomSheet != null && mHistoryBottomSheet.isShowing())) {
            return;
        }

        String content = getNoteContentString();
        String tagString = getNoteTagsString();
        if (mNote.hasChanges(content, tagString.trim(), mPinButton.isChecked())) {
            mNote.setContent(content);
            mNote.setTagString(tagString);
            mNote.setModificationDate(Calendar.getInstance());
            // Send pinned event to google analytics if changed
            mNote.setPinned(mPinButton.isChecked());
            mNote.save();
            if (getActivity() != null) {
                if (mNote.isPinned() != mPinButton.isChecked()) {
                    AnalyticsTracker.track(
                            mPinButton.isChecked() ? AnalyticsTracker.Stat.EDITOR_NOTE_PINNED :
                                    AnalyticsTracker.Stat.EDITOR_NOTE_UNPINNED,
                            AnalyticsTracker.CATEGORY_NOTE,
                            "pin_button"
                    );
                }

                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.EDITOR_NOTE_EDITED,
                        AnalyticsTracker.CATEGORY_NOTE,
                        "editor_save"
                );
            }
        }
    }

    // Contextual action bar for dealing with links
    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            if (inflater != null) {
                inflater.inflate(R.menu.view_link, menu);
                mViewLinkMenuItem = menu.findItem(R.id.menu_view_link);
                mode.setTitle(getString(R.string.link));
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    mode.setTitleOptionalHint(false);
                }
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
                    if (mLinkText != null && getActivity() != null) {
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
            if (noteContent == null)
                return;

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
                if (getActivity() != null) {
                    mActionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(mActionModeCallback);
                    if (mActionMode != null) {
                        mActionMode.setSubtitle(mLinkText);
                    }

                    setLinkMenuItem();
                }
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
                mViewLinkMenuItem.setIcon(mCallIconResId);
                mViewLinkMenuItem.setTitle(getString(R.string.call));
            } else if (mLinkUrl.startsWith("mailto:")) {
                mViewLinkMenuItem.setIcon(mEmailIconResId);
                mViewLinkMenuItem.setTitle(getString(R.string.email));
            } else if (mLinkUrl.startsWith("geo:")) {
                mViewLinkMenuItem.setIcon(mMapIconResId);
                mViewLinkMenuItem.setTitle(getString(R.string.view_map));
            } else {
                mViewLinkMenuItem.setIcon(mWebIconResId);
                mViewLinkMenuItem.setTitle(getString(R.string.view_in_browser));
            }
        }
    }

    // Resets note publish status if Simperium never returned the new publish status
    private final Runnable mPublishTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            getActivity(),
                            mNote.isPublished() ? R.string.publish_error : R.string.unpublish_error,
                            Toast.LENGTH_SHORT
                    ).show();

                    mNote.setPublished(!mNote.isPublished());
                    mNote.save();

                    updatePublishedState(false);

                }
            });

        }
    };

    private String getWordCount() {
        String content = getNoteContentString();

        if (TextUtils.isEmpty(content)) return "";

        int numWords = (content.trim().length() == 0) ? 0 : content.trim().split("([\\W]+)").length;

        String wordCountString = getResources().getQuantityString(R.plurals.word_count, numWords);
        String formattedWordCount = NumberFormat.getInstance().format(numWords);

        return String.format("%s %s", formattedWordCount, wordCountString);
    }

    private void setPublishedNote(boolean isPublished) {
        if (mNote != null) {
            mNote.setPublished(isPublished);
            mNote.save();

            // reset publish status in 20 seconds if we don't hear back from Simperium
            mPublishTimeoutHandler.postDelayed(mPublishTimeoutRunnable, 20000);

            AnalyticsTracker.track(
                    (isPublished) ? AnalyticsTracker.Stat.EDITOR_NOTE_PUBLISHED :
                            AnalyticsTracker.Stat.EDITOR_NOTE_UNPUBLISHED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "publish_note_button"
            );
        }
    }

    public View getSnackbarView() {
        return getActivity().findViewById(android.R.id.content);
    }

    private Snackbar showSnackbar(@StringRes int message, @ColorRes int color, int duration) {
        if (isAdded()) {
            Snackbar snackbar = Snackbar.make(getSnackbarView(), message, duration);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(getActivity(), color));
            snackbar.show();
            return snackbar;
        }
        return null;
    }

    private Snackbar showSnackbar(@StringRes int message, @ColorRes int color, int duration,
                                  @StringRes int action, View.OnClickListener onClick) {
        if (isAdded()) {
            Snackbar snackbar = Snackbar.make(getSnackbarView(), message, duration);
            snackbar.setAction(action, onClick);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(getActivity(), color));
            snackbar.show();
            return snackbar;
        }
        return null;
    }

    private void updatePublishedState(boolean isSuccess) {

        if (mPublishingSnackbar != null) {
            mPublishingSnackbar.dismiss();
        }

//        if (mShareBottomSheet != null) {
//
//        }
//
//        if (isSuccess) {
//            mPublishingSnackbar = showSnackbar(R.string.published, R.color.holo_green_light,
//                    Snackbar.LENGTH_LONG, R.string.undo, new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            setPublishedNote(!mNote.isPublished());
//                        }
//                    });
//        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.app_name), text);
        clipboard.setPrimaryClip(clip);
    }

    private void showShareSheet() {
        mShareBottomSheet = new BottomSheetDialog(getActivity());

        View shareView = LayoutInflater.from(getActivity()).inflate(R.layout.bottom_sheet_share, null, false);
        TextView collaborateButton = (TextView)shareView.findViewById(R.id.share_collaborate_button);
        TextView publishButton = (TextView)shareView.findViewById(R.id.share_publish_button);

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mPublishingSnackbar = showSnackbar(R.string.publishing, R.color.simplenote_blue_accent,
                        Snackbar.LENGTH_INDEFINITE, R.string.cancel, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setPublishedNote(!mNote.isPublished());
                            }
                        });
                setPublishedNote(true);
            }
        });

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mNote.getContent());

        final List<ResolveInfo> matches = getActivity().getPackageManager().queryIntentActivities(intent, 0);
        List<ShareButtonAdapter.ShareButtonItem> shareButtons = new ArrayList<>();
        for (ResolveInfo match : matches) {
            final Drawable icon = match.loadIcon(getActivity().getPackageManager());
            final CharSequence label = match.loadLabel(getActivity().getPackageManager());
            shareButtons.add(new ShareButtonAdapter.ShareButtonItem(icon, label, match.activityInfo.packageName));
        }

        RecyclerView recyclerView = (RecyclerView) shareView.findViewById(R.id.share_button_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        ShareButtonAdapter.ItemListener shareListener = new ShareButtonAdapter.ItemListener() {
            @Override
            public void onItemClick(ShareButtonAdapter.ShareButtonItem item) {
                intent.setPackage(item.getPackageName());
                startActivity(Intent.createChooser(intent, getString(R.string.share)));
                mShareBottomSheet.dismiss();
            }
        };

        recyclerView.setAdapter(new ShareButtonAdapter(shareButtons, shareListener));

        mShareBottomSheet.setContentView(shareView);
        mShareBottomSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mShareBottomSheet = null;
            }
        });
        mShareBottomSheet.show();
    }

    private void showInfoSheet() {
        mInfoBottomSheet = new BottomSheetDialog(getActivity());
        View infoView = LayoutInflater.from(getActivity()).inflate(R.layout.bottom_sheet_info, null, false);

        TextView infoModifiedDate = (TextView)infoView.findViewById(R.id.info_modified_date_text);
        TextView infoWords = (TextView)infoView.findViewById(R.id.info_words_text);
        TextView infoLinkUrl = (TextView)infoView.findViewById(R.id.info_public_link_url);
        TextView infoLinkTitle = (TextView)infoView.findViewById(R.id.info_public_link_title);

        Switch infoPinSwitch = (Switch)infoView.findViewById(R.id.info_pin_switch);
        infoPinSwitch.setChecked(mPinButton.isChecked());
        infoPinSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPinButton.setChecked(isChecked);
            }
        });

        Switch infoMarkdownSwitch = (Switch)infoView.findViewById(R.id.info_markdown_switch);
//        infoMarkdownSwitch.setChecked(mPinButton.isChecked());
        infoMarkdownSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });

        ImageButton copyButton = (ImageButton)infoView.findViewById(R.id.info_copy_link_button);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(mNote.getPublishedUrl());
                Toast.makeText(getActivity(), getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
            }
        });


        ImageButton shareButton = (ImageButton)infoView.findViewById(R.id.info_share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInfoBottomSheet.dismiss();
                showShareSheet();
            }
        });

        if (mNote != null) {
            String date = getHistoryDateText(mNote.getModificationDate());
            infoModifiedDate.setText(String.format(getString(R.string.modified_time), date));

            infoWords.setText(getWordCount());

            if (mNote.isPublished()) {
                infoLinkUrl.setText(mNote.getPublishedUrl());
                shareButton.setVisibility(View.GONE);
                copyButton.setVisibility(View.VISIBLE);

            } else {
                infoLinkTitle.setText(getString(R.string.publish));
                copyButton.setVisibility(View.GONE);
                shareButton.setVisibility(View.VISIBLE);
            }
        }

        mInfoBottomSheet.setContentView(infoView);
        mInfoBottomSheet.show();
        mInfoBottomSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mInfoBottomSheet = null;
            }
        });
    }

    /**
     * History methods
     */
    private void showHistorySheet() {
        mContentEditText.clearFocus();

        mHistoryBottomSheet = new BottomSheetDialog(getActivity());

        mHistoryView = LayoutInflater.from(getActivity()).inflate(R.layout.bottom_sheet_history, null, false);
        mHistoryDate = (TextView)mHistoryView.findViewById(R.id.history_date);
        mHistorySeekBar = (SeekBar) mHistoryView.findViewById(R.id.seek_bar);
        mHistorySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mNoteRevisionsList == null  || mHistoryBottomSheet == null || !mHistoryBottomSheet.isShowing()) {
                    return;
                }

                Calendar noteDate = null;
                if (progress == mNoteRevisionsList.size()) {
                    mContentEditText.setText(mNote.getContent());
                    noteDate = mNote.getModificationDate();
                } else if (progress < mNoteRevisionsList.size() && mNoteRevisionsList.get(progress) != null) {
                    Note revisedNote = mNoteRevisionsList.get(progress);
                    noteDate = revisedNote.getModificationDate();
                    mContentEditText.setText(revisedNote.getContent());
                }

                mHistoryDate.setText(getHistoryDateText(noteDate));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // noop
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // noop
            }
        });

        View cancelHistoryButton = mHistoryView.findViewById(R.id.cancel_history_button);
        cancelHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContentEditText.setText(mNote.getContent());
                if (mHistoryBottomSheet != null) {
                    mDidTapHistoryButton = true;
                    mHistoryBottomSheet.dismiss();
                }
            }
        });

        View restoreHistoryButton = mHistoryView.findViewById(R.id.restore_history_button);
        restoreHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHistoryBottomSheet != null) {
                    mDidTapHistoryButton = true;
                    mHistoryBottomSheet.dismiss();
                }
                saveAndSyncNote();
            }
        });

        updateHistoryProgressBar();

        mHistoryBottomSheet = new BottomSheetDialog(getActivity());
        mHistoryBottomSheet.setContentView(mHistoryView);
        mHistoryBottomSheet.show();
        mHistoryBottomSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!mDidTapHistoryButton) {
                    mContentEditText.setText(mNote.getContent());
                }

                mDidTapHistoryButton = false;
                mHistoryBottomSheet = null;
            }
        });
    }

    private String getHistoryDateText(Calendar noteDate) {
        if (!isAdded() || noteDate == null) {
            return "";
        }

        long now = Calendar.getInstance().getTimeInMillis();
        CharSequence dateText = DateUtils.getRelativeDateTimeString(
                getActivity(),
                noteDate.getTimeInMillis(),
                now,
                0L,
                DateUtils.FORMAT_ABBREV_ALL
        );

        return dateText.toString();
    }

    private void updateHistoryProgressBar() {
        if (mHistorySeekBar == null) return;

        int totalRevs = mNoteRevisionsList == null ? 0 : mNoteRevisionsList.size();
        if (totalRevs > 0) {
            mHistorySeekBar.setMax(totalRevs);
            mHistorySeekBar.setProgress(totalRevs);

            mHistoryDate.setText(getHistoryDateText(mNote.getModificationDate()));

            mHistoryView.findViewById(R.id.history_loading_view).setVisibility(View.GONE);
            mHistoryView.findViewById(R.id.history_slider_view).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Simperium listeners
     */

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {

    }

    @Override
    public void onNetworkChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, final String key) {
        if (changeType == Bucket.ChangeType.MODIFY) {
            if (getNote() != null && getNote().getSimperiumKey().equals(key)) {
                try {
                    final Note updatedNote = mNotesBucket.get(key);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mPublishTimeoutHandler != null) {
                                    mPublishTimeoutHandler.removeCallbacks(mPublishTimeoutRunnable);
                                }

                                updateNote(updatedNote);
                                updatePublishedState(true);
                            }
                        });
                    }
                } catch (BucketObjectMissingException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> bucket, Note note) {
        // Don't apply updates if we haven't loaded the note yet
        if (mIsLoadingNote)
            return;

        Note openNote = getNote();
        if (openNote == null || !openNote.getSimperiumKey().equals(note.getSimperiumKey()))
            return;

        note.setContent(getNoteContentString());
    }

    private final Bucket.RevisionsRequestCallbacks<Note> mRevisionsRequestCallbacks = new
            Bucket.RevisionsRequestCallbacks<Note>() {
                // Note: These callbacks won't be running on the main thread
                @Override
                public void onComplete(Map<Integer, Note> revisionsMap) {
                    if (!isAdded()) return;

                    // Convert map to an array list, to work better with the 0-index based seekbar
                    mNoteRevisionsList = new ArrayList<>(revisionsMap.values());
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateHistoryProgressBar();
                        }
                    });
                }

                @Override
                public void onRevision(String key, int version, JSONObject object) {}

                @Override
                public void onError(Throwable exception) {
                    if (!isAdded() || mHistoryView == null) return;

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mHistoryView.findViewById(R.id.history_progress_bar).setVisibility(View.GONE);
                            mHistoryView.findViewById(R.id.history_error_text).setVisibility(View.VISIBLE);
                        }
                    });
                }
            };
}
