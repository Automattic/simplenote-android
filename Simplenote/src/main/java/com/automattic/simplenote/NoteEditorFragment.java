package com.automattic.simplenote;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
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
import android.webkit.WebView;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.AutoBullet;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.MatchOffsetHighlighter;
import com.automattic.simplenote.utils.NoteUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.SimplenoteLinkify;
import com.automattic.simplenote.utils.SnackbarUtils;
import com.automattic.simplenote.utils.SpaceTokenizer;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView.OnTagAddedListener;
import com.automattic.simplenote.utils.TextHighlighter;
import com.automattic.simplenote.widgets.SimplenoteEditText;
import com.commonsware.cwac.anddown.AndDown;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class NoteEditorFragment extends Fragment implements Bucket.Listener<Note>,
        TextWatcher, OnTagAddedListener, View.OnFocusChangeListener,
        SimplenoteEditText.OnSelectionChangedListener,
        ShareBottomSheetDialog.ShareSheetListener,
        HistoryBottomSheetDialog.HistorySheetListener,
        InfoBottomSheetDialog.InfoSheetListener,
        ReminderBottomSheetDialog.ReminderSheetListener {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_NEW_NOTE = "new_note";
    static public final String ARG_MATCH_OFFSETS = "match_offsets";
    static public final String ARG_MARKDOWN_ENABLED = "markdown_enabled";
    private static final int AUTOSAVE_DELAY_MILLIS = 2000;
    private static final int MAX_REVISIONS = 30;
    private static final int PUBLISH_TIMEOUT = 20000;
    private static final int HISTORY_TIMEOUT = 10000;
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    private Note mNote;
    private Bucket<Note> mNotesBucket;

    private SimplenoteEditText mContentEditText;
    private TagsMultiAutoCompleteTextView mTagView;

    private Handler mAutoSaveHandler;
    private Handler mPublishTimeoutHandler;
    private Handler mHistoryTimeoutHandler;

    private LinearLayout mPlaceholderView;
    private CursorAdapter mAutocompleteAdapter;
    private boolean mIsNewNote, mIsLoadingNote, mIsMarkdownEnabled, mHasReminder, mHasReminderDateChange;
    private ActionMode mActionMode;
    private MenuItem mViewLinkMenuItem;
    private String mLinkUrl;
    private String mLinkText;
    private MatchOffsetHighlighter mHighlighter;
    private Drawable mEmailIcon, mWebIcon, mMapIcon, mCallIcon;
    private MatchOffsetHighlighter.SpanFactory mMatchHighlighter;
    private String mMatchOffsets;
    private int mCurrentCursorPosition;

    private HistoryBottomSheetDialog mHistoryBottomSheet;
    private InfoBottomSheetDialog mInfoBottomSheet;
    private ShareBottomSheetDialog mShareBottomSheet;
    private ReminderBottomSheetDialog mReminderBottomSheet;

    private Snackbar mPublishingSnackbar;
    private boolean mIsUndoingPublishing;

    private NoteMarkdownFragment mNoteMarkdownFragment;
    private String mCss;
    private WebView mMarkdown;

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
        }

        mCallIcon = DrawableUtils.tintDrawableWithAttribute(getActivity(), R.drawable.ic_call_white_24dp, R.attr.actionModeTextColor);
        mEmailIcon = DrawableUtils.tintDrawableWithAttribute(getActivity(), R.drawable.ic_email_white_24dp, R.attr.actionModeTextColor);
        mMapIcon = DrawableUtils.tintDrawableWithAttribute(getActivity(), R.drawable.ic_map_white_24dp, R.attr.actionModeTextColor);
        mWebIcon = DrawableUtils.tintDrawableWithAttribute(getActivity(), R.drawable.ic_web_white_24dp, R.attr.actionModeTextColor);

        mAutoSaveHandler = new Handler();
        mPublishTimeoutHandler = new Handler();
        mHistoryTimeoutHandler = new Handler();

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
            public CharSequence convertToString(Cursor cursor) {
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
                if (filter != null)
                    query.where(Tag.NAME_PROPERTY, Query.ComparisonType.LIKE, String.format("%s%%", filter));
                return query.execute();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_note_editor, container, false);
        mContentEditText = ((SimplenoteEditText) rootView.findViewById(R.id.note_content));
        mContentEditText.addOnSelectionChangedListener(this);
        mTagView = (TagsMultiAutoCompleteTextView) rootView.findViewById(R.id.tag_view);
        mTagView.setTokenizer(new SpaceTokenizer());
        mTagView.setOnFocusChangeListener(this);

        mHighlighter = new MatchOffsetHighlighter(mMatchHighlighter, mContentEditText);

        mPlaceholderView = (LinearLayout) rootView.findViewById(R.id.placeholder);
        if (DisplayUtils.isLargeScreenLandscape(getActivity()) && mNote == null) {
            mPlaceholderView.setVisibility(View.VISIBLE);
            getActivity().invalidateOptionsMenu();
            mMarkdown = (WebView) rootView.findViewById(R.id.markdown);

            switch (PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_THEME, THEME_LIGHT)) {
                case THEME_DARK:
                    mCss = "<link rel=\"stylesheet\" type=\"text/css\" href=\"dark.css\" />";
                    break;
                case THEME_LIGHT:
                    mCss = "<link rel=\"stylesheet\" type=\"text/css\" href=\"light.css\" />";
                    break;
            }
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

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mNotesBucket.start();
        mNotesBucket.addListener(this);

        mTagView.setOnTagAddedListener(this);

        if (mContentEditText != null) {
            mContentEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_FONT_SIZE, 14));
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

        if (mHistoryTimeoutHandler != null) {
            mHistoryTimeoutHandler.removeCallbacks(mHistoryTimeoutRunnable);
        }

        mHighlighter.stop();

        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded() || DisplayUtils.isLargeScreenLandscape(getActivity()) && mNoteMarkdownFragment == null) {
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

        DrawableUtils.tintMenuWithAttribute(getActivity(), menu, R.attr.actionBarTextColor);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reminder:
                setReminder();
                return true;
            case R.id.menu_view_info:
                showInfo();
                return true;
            case R.id.menu_history:
                showHistory();
                return true;
            case R.id.menu_share:
                shareNote();
                return true;
            case R.id.menu_delete:
                if (!isAdded()) return false;
                deleteNote();
                return true;
            case android.R.id.home:
                if (!isAdded()) return false;
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteNote() {
        NoteUtils.deleteNote(mNote, getActivity());
        getActivity().finish();
    }

    protected void clearMarkdown() {
        mMarkdown.loadDataWithBaseURL("file:///android_asset/", mCss + "", "text/html", "utf-8", null);
    }

    protected void hideMarkdown() {
        mMarkdown.setVisibility(View.INVISIBLE);
    }

    protected void showMarkdown() {
        loadMarkdownData();
        mMarkdown.setVisibility(View.VISIBLE);
    }

    private void shareNote() {
        if (mNote != null) {
            mContentEditText.clearFocus();
            showShareSheet();
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_NOTE_CONTENT_SHARED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "action_bar_share_button"
            );
        }
    }

    private void showHistory() {
        if (mNote != null && mNote.getVersion() > 1) {
            mContentEditText.clearFocus();
            mHistoryTimeoutHandler.postDelayed(mHistoryTimeoutRunnable, HISTORY_TIMEOUT);
            showHistorySheet();
        } else {
            Toast.makeText(getActivity(), R.string.error_history, Toast.LENGTH_LONG).show();
        }
    }

    private void showInfo() {
        if (mNote != null) {
            mContentEditText.clearFocus();
            saveNote();
            showInfoSheet();
        }
    }

    private void setReminder() {
        if (mNote != null) {
            mContentEditText.clearFocus();
            showReminderPopUp();
        }
    }

    private void showReminderPopUp() {
        if (isAdded()) {
            if (mReminderBottomSheet == null) {
                mReminderBottomSheet = new ReminderBottomSheetDialog(this, this);
            }
            mReminderBottomSheet.show(mNote);
        }
    }

    private boolean noteIsEmpty() {
        return (getNoteContentString().trim().length() == 0 && getNoteTagsString().trim().length() == 0);
    }

    protected void setMarkdownEnabled(boolean enabled) {
        mIsMarkdownEnabled = enabled;

        if (mIsMarkdownEnabled) {
            loadMarkdownData();
        }
    }

    private void loadMarkdownData() {
        mMarkdown.loadDataWithBaseURL("file:///android_asset/", mCss +
                new AndDown().markdownToHtml(getNoteContentString()), "text/html", "utf-8", null);
    }

    public void setNote(String noteID) {
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
        } else {
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
        attemptAutoList(editable);
        setTitleSpan(editable);
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

    /**
     * Share bottom sheet callbacks
     */

    @Override
    public void onSharePublishClicked() {
        publishNote();
        mShareBottomSheet.dismiss();
    }

    @Override
    public void onShareUnpublishClicked() {
        unpublishNote();
        mShareBottomSheet.dismiss();
    }

    @Override
    public void onShareCollaborateClicked() {
        Toast.makeText(getActivity(), R.string.collaborate_message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onShareDismissed() {

    }

    /**
     * History bottom sheet listeners
     */

    @Override
    public void onHistoryCancelClicked() {
        mContentEditText.setText(mNote.getContent());
        mHistoryBottomSheet.dismiss();
    }

    @Override
    public void onHistoryRestoreClicked() {
        mHistoryBottomSheet.dismiss();
        saveAndSyncNote();
    }

    @Override
    public void onHistoryDismissed() {
        if (!mHistoryBottomSheet.didTapOnButton()) {
            mContentEditText.setText(mNote.getContent());
        }

        if (mHistoryTimeoutHandler != null) {
            mHistoryTimeoutHandler.removeCallbacks(mHistoryTimeoutRunnable);
        }
    }

    @Override
    public void onHistoryUpdateNote(String content) {
        mContentEditText.setText(content);
    }

    /**
     * Info bottom sheet listeners
     */

    @Override
    public void onInfoPinSwitchChanged(boolean isSwitchedOn) {
        NoteUtils.setNotePin(mNote, isSwitchedOn);
    }

    @Override
    public void onInfoMarkdownSwitchChanged(boolean isSwitchedOn) {
        mIsMarkdownEnabled = isSwitchedOn;
        Activity activity = getActivity();

        if (activity instanceof NoteEditorActivity) {

            NoteEditorActivity editorActivity = (NoteEditorActivity) activity;
            if (mIsMarkdownEnabled) {

                editorActivity.showTabs();

                if (mNoteMarkdownFragment == null) {
                    // Get markdown fragment and update content
                    mNoteMarkdownFragment =
                            editorActivity.getNoteMarkdownFragment();
                    mNoteMarkdownFragment.updateMarkdown(getNoteContentString());
                }
            } else {
                editorActivity.hideTabs();
            }
        } else if (activity instanceof NotesActivity) {
            setMarkdownEnabled(mIsMarkdownEnabled);
            ((NotesActivity) getActivity()).setMarkdownShowing(false);
        }

        saveNote();
    }

    @Override
    public void onInfoCopyLinkClicked() {
        copyToClipboard(mNote.getPublishedUrl());
        Toast.makeText(getActivity(), getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoShareLinkClicked() {
        mInfoBottomSheet.dismiss();
        showShareSheet();
    }

    @Override
    public void onInfoDismissed() {
        mInfoBottomSheet.dismiss();
    }

    @Override
    public void onReminderOn() {
        mHasReminder = true;
        saveNote();
    }

    @Override
    public void onReminderOff() {
        mHasReminder = false;
        saveNote();
    }

    @Override
    public void onReminderUpdated(Calendar calendar) {
        mNote.setReminderDate(calendar);
        mHasReminderDateChange = true;
        mReminderBottomSheet.updateReminder(calendar);
    }

    @Override
    public void onReminderDismissed() {
        mReminderBottomSheet.dismiss();
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

                // Set markdown flag for current note
                if (mNote != null) {
                    mIsMarkdownEnabled = mNote.isMarkdownEnabled();
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

            // Show tabs if markdown is enabled globally, for current note, and not tablet landscape
            if (mIsMarkdownEnabled) {
                // Get markdown view and update content
                if (DisplayUtils.isLargeScreenLandscape(getActivity())) {
                    loadMarkdownData();
                } else {
                    mNoteMarkdownFragment =
                            ((NoteEditorActivity) getActivity()).getNoteMarkdownFragment();
                    mNoteMarkdownFragment.updateMarkdown(getNoteContentString());
                    ((NoteEditorActivity) getActivity()).showTabs();
                }
            }

            getActivity().invalidateOptionsMenu();

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

                // Update markdown fragment
                if (DisplayUtils.isLargeScreenLandscape(getActivity())) {
                    loadMarkdownData();
                } else if (mNoteMarkdownFragment != null) {
                    mNoteMarkdownFragment.updateMarkdown(getNoteContentString());
                }
            }
        }
    }

    protected void saveNote() {
        if (mNote == null || (mHistoryBottomSheet != null && mHistoryBottomSheet.isShowing())) {
            return;
        }

        String content = getNoteContentString();
        String tagString = getNoteTagsString();
        if (mHasReminderDateChange || mNote.hasChanges(content, tagString.trim(), mNote.isPinned(), mIsMarkdownEnabled, mHasReminder)) {
            mNote.setContent(content);
            mNote.setTagString(tagString);
            mNote.setModificationDate(Calendar.getInstance());
            mNote.setMarkdownEnabled(mIsMarkdownEnabled);
            mNote.setReminder(mHasReminder);
            // Send pinned event to google analytics if changed
            mNote.save();

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_NOTE_EDITED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "editor_save"
            );
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

                DrawableUtils.tintMenuWithAttribute(getActivity(), menu, R.attr.actionModeTextColor);
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
                        copyToClipboard(mLinkText);
                        Toast.makeText(getActivity(), getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
                        mode.finish();
                    }
                    return true;
                case R.id.menu_share:
                    if (mLinkText != null) {
                        showShareSheet();
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
                    mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
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
                mViewLinkMenuItem.setIcon(mCallIcon);
                mViewLinkMenuItem.setTitle(getString(R.string.call));
            } else if (mLinkUrl.startsWith("mailto:")) {
                mViewLinkMenuItem.setIcon(mEmailIcon);
                mViewLinkMenuItem.setTitle(getString(R.string.email));
            } else if (mLinkUrl.startsWith("geo:")) {
                mViewLinkMenuItem.setIcon(mMapIcon);
                mViewLinkMenuItem.setTitle(getString(R.string.view_map));
            } else {
                mViewLinkMenuItem.setIcon(mWebIcon);
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

                    mNote.setPublished(!mNote.isPublished());
                    mNote.save();

                    updatePublishedState(false);

                }
            });

        }
    };

    // Hides the history bottom sheet if no revisions are loaded
    private final Runnable mHistoryTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (mHistoryBottomSheet.isShowing() && !mHistoryBottomSheet.isHistoryLoaded()) {
                        mHistoryBottomSheet.dismiss();
                        Toast.makeText(getActivity(), R.string.error_history, Toast.LENGTH_LONG).show();
                    }
                }
            });

        }
    };

    private void setPublishedNote(boolean isPublished) {
        if (mNote != null) {
            mNote.setPublished(isPublished);
            mNote.save();

            // reset publish status in 20 seconds if we don't hear back from Simperium
            mPublishTimeoutHandler.postDelayed(mPublishTimeoutRunnable, PUBLISH_TIMEOUT);

            AnalyticsTracker.track(
                    (isPublished) ? AnalyticsTracker.Stat.EDITOR_NOTE_PUBLISHED :
                            AnalyticsTracker.Stat.EDITOR_NOTE_UNPUBLISHED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "publish_note_button"
            );
        }
    }

    private void updatePublishedState(boolean isSuccess) {

        if (mPublishingSnackbar == null) {
            return;
        }

        mPublishingSnackbar.dismiss();
        mPublishingSnackbar = null;

        if (isSuccess && isAdded()) {
            if (mNote.isPublished()) {

                if (mIsUndoingPublishing) {
                    SnackbarUtils.showSnackbar(getActivity(), R.string.publish_successful,
                            R.color.simplenote_positive_green,
                            Snackbar.LENGTH_LONG);
                } else {
                    SnackbarUtils.showSnackbar(getActivity(), R.string.publish_successful,
                            R.color.simplenote_positive_green,
                            Snackbar.LENGTH_LONG, R.string.undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mIsUndoingPublishing = true;
                                    unpublishNote();
                                }
                            });
                }
                copyToClipboard(mNote.getPublishedUrl());
            } else {
                if (mIsUndoingPublishing) {
                    SnackbarUtils.showSnackbar(getActivity(), R.string.unpublish_successful,
                            R.color.simplenote_negative_red,
                            Snackbar.LENGTH_LONG);
                } else {
                    SnackbarUtils.showSnackbar(getActivity(), R.string.unpublish_successful,
                            R.color.simplenote_negative_red,
                            Snackbar.LENGTH_LONG, R.string.undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mIsUndoingPublishing = true;
                                    publishNote();
                                }
                            });
                }
            }
        } else {
            if (mNote.isPublished()) {
                SnackbarUtils.showSnackbar(getActivity(), R.string.unpublish_error,
                        R.color.simplenote_negative_red, Snackbar.LENGTH_LONG);
            } else {
                SnackbarUtils.showSnackbar(getActivity(), R.string.publish_error,
                        R.color.simplenote_negative_red, Snackbar.LENGTH_LONG);
            }
        }

        mIsUndoingPublishing = false;
    }

    private void publishNote() {

        if (isAdded()) {
            mPublishingSnackbar = SnackbarUtils.showSnackbar(getActivity(), R.string.publishing,
                    R.color.simplenote_blue, Snackbar.LENGTH_INDEFINITE);
        }
        setPublishedNote(true);
    }

    private void unpublishNote() {

        if (isAdded()) {
            mPublishingSnackbar = SnackbarUtils.showSnackbar(getActivity(), R.string.unpublishing,
                    R.color.simplenote_blue, Snackbar.LENGTH_INDEFINITE);
        }
        setPublishedNote(false);
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.app_name), text);
        clipboard.setPrimaryClip(clip);
    }

    private void showShareSheet() {
        if (isAdded()) {
            if (mShareBottomSheet == null) {
                mShareBottomSheet = new ShareBottomSheetDialog(this, this);
            }
            mShareBottomSheet.show(mNote);
        }
    }

    private void showInfoSheet() {
        if (isAdded()) {
            if (mInfoBottomSheet == null) {
                mInfoBottomSheet = new InfoBottomSheetDialog(this, this);
            }
            mInfoBottomSheet.show(mNote);
        }

    }

    private void showHistorySheet() {
        if (isAdded()) {
            if (mHistoryBottomSheet == null) {
                mHistoryBottomSheet = new HistoryBottomSheetDialog(this, this);
            }

            // Request revisions for the current note
            mNotesBucket.getRevisions(mNote, MAX_REVISIONS, mHistoryBottomSheet.getRevisionsRequestCallbacks());
            saveNote();

            mHistoryBottomSheet.show(mNote);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ReminderBottomSheetDialog.UPDATE_REMINDER_REQUEST_CODE) {
            long timestamp = data.getLongExtra(ReminderBottomSheetDialog.TIMESTAMP_BUNDLE_KEY, 0);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(new Date(timestamp));

            onReminderUpdated(calendar);
        }
    }
}