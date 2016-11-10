package com.automattic.simplenote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.AlarmUtils;
import com.automattic.simplenote.utils.AutoBullet;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.MatchOffsetHighlighter;
import com.automattic.simplenote.utils.NoteUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.SimplenoteLinkify;
import com.automattic.simplenote.utils.SnackbarUtils;
import com.automattic.simplenote.utils.SpaceTokenizer;
import com.automattic.simplenote.utils.StrUtils;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView;
import com.automattic.simplenote.utils.TagsMultiAutoCompleteTextView.OnTagAddedListener;
import com.automattic.simplenote.utils.TextHighlighter;
import com.automattic.simplenote.widgets.SimplenoteEditText;
import com.commonsware.cwac.anddown.AndDown;
import com.mobeta.android.dslv.DragSortListView;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;

import org.dmfs.android.colorpicker.ColorPickerDialogFragment.ColorDialogResultListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class NoteEditorFragment extends Fragment implements Bucket.Listener<Note>,
        TextWatcher, OnTagAddedListener, View.OnFocusChangeListener,
        SimplenoteEditText.OnSelectionChangedListener,
        ShareBottomSheetDialog.ShareSheetListener,
        HistoryBottomSheetDialog.HistorySheetListener,
        InfoBottomSheetDialog.InfoSheetListener,
        ReminderBottomSheetDialog.ReminderSheetListener,
        ColorBottomSheetDialog.ColorSheetListener,
        ColorDialogResultListener
{

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_NEW_NOTE = "new_note";
    static public final String ARG_MATCH_OFFSETS = "match_offsets";
    static public final String ARG_MARKDOWN_ENABLED = "markdown_enabled";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    private static final int AUTOSAVE_DELAY_MILLIS = 2000;
    private static final int MAX_REVISIONS = 30;
    private static final int PUBLISH_TIMEOUT = 20000;
    private static final int HISTORY_TIMEOUT = 10000;
    private Note mNote;
    private final Runnable mAutoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveAndSyncNote();
        }
    };
    private Bucket<Note> mNotesBucket;
    private SimplenoteEditText mContentEditText;
    private TagsMultiAutoCompleteTextView mTagView;
    private Handler mAutoSaveHandler;
    private Handler mPublishTimeoutHandler;
    private Handler mHistoryTimeoutHandler;
    private LinearLayout mPlaceholderView;
    private CursorAdapter mAutocompleteAdapter;
    private boolean mIsNewNote, mIsLoadingNote, mIsMarkdownEnabled, mHasReminder, mHasReminderDateChange;
    private boolean mColor;
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
    private InfoBottomSheetDialog mInfoBottomSheet;
    private ShareBottomSheetDialog mShareBottomSheet;
    private ReminderBottomSheetDialog mReminderBottomSheet;
    private ColorBottomSheetDialog mColorBottomSheet;

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
    private Snackbar mPublishingSnackbar;
    private boolean mIsUndoingPublishing;
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
    private NoteMarkdownFragment mNoteMarkdownFragment;
    private String mCss;
    private WebView mMarkdown;
    private String mKey;
    private View mColorIndicator;

    private MenuItem mPinnerItem;
    private MenuItem mMarkdownItem;
    private MenuItem mTemplateItem;

    private EditText mAddTodoText;
    private Boolean mIsTodo;
    private ArrayList<String> mTodos;
    private ArrayList<String> mTodosCompleted;
    private DragSortListView mTodoList;
    private DragSortListView mCompletedTodoList;
    private View mTodoDivider;
    private LinearLayout mTodoComponent;
    private JSONAdapter jSONAdapter ;

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

        mColorIndicator = rootView.findViewById(R.id.color_indicator);
        // Load note if we were passed a note Id
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            mKey = arguments.getString(ARG_ITEM_ID);
            if (arguments.containsKey(ARG_MATCH_OFFSETS)) {
                mMatchOffsets = arguments.getString(ARG_MATCH_OFFSETS);
            }
            new loadNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mKey);
            setIsNewNote(getArguments().getBoolean(ARG_NEW_NOTE, false));
        }

        mTodoList = (DragSortListView) rootView.findViewById(R.id.todo_list);
        mCompletedTodoList = (DragSortListView) rootView.findViewById(R.id.todo_list_completed);
        mAddTodoText = (EditText) rootView.findViewById(R.id.todo_add_text);
        mTodoComponent = (LinearLayout) rootView.findViewById(R.id.todo_component);
        mTodoDivider = rootView.findViewById(R.id.todo_divider);

        mAddTodoText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0)
                    if (s.charAt(s.length() - 1) == '\n') {
                        String todoText = mAddTodoText.getText().toString();
                        if (todoText.length() != 0) {
                            mTodos.add(0, todoText);
                            mNote.setTodos(mTodos);

                            mNote.save();
                            updateTodos();
                            mAddTodoText.setText("");
                        }
                    }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        mTodoList.setDropListener(new DragSortListView.DropListener() {
            @Override public void drop(int from, int to) {
                String item = mTodos.get(from);
                mTodos.remove(from);
                if (from > to) --from;
                mTodos.add(to, item);

                mNote.setTodos(mTodos);
                mNote.setCompletedTodos(mTodosCompleted);
                mNote.save();
                updateTodos();
            }
        });

        mCompletedTodoList.setDropListener(new DragSortListView.DropListener() {
            @Override public void drop(int from, int to) {
                String item = mTodosCompleted.get(from);
                mTodosCompleted.remove(from);
                if (from > to) --from;
                mTodosCompleted.add(to, item);

                mNote.setTodos(mTodos);
                mNote.setCompletedTodos(mTodosCompleted);
                mNote.save();
                updateTodos();
            }
        });




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

            int color = mNote.getColor();
            mColorIndicator.setBackgroundColor(color);

            if (color != Color.WHITE)
                mColorIndicator.setVisibility(View.VISIBLE);


            mTodos = mNote.getTodos();
            mTodosCompleted = mNote.getCompletedTodos();
            updateTodos();

            if (mNote.isTodo() == true) {
                mTodoComponent.setVisibility(View.VISIBLE);
                //mContentEditText.setMaxLines(1);
                mContentEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                //mContentEditText.setEllipsize(TextUtils.TruncateAt.END);
                cutContent();
            }
        }

        mPinnerItem = (MenuItem) menu.findItem(R.id.info_pin_switch_menu);
        mPinnerItem.setTitle(mNote.isPinned()? R.string.unpin_from_top : R.string.pin_to_top);

        mMarkdownItem = (MenuItem) menu.findItem(R.id.info_markdown_menu);
        mMarkdownItem.setTitle(mNote.isMarkdownEnabled() ? R.string.markdown_hide : R.string.markdown_show);

        mTemplateItem = (MenuItem) menu.findItem(R.id.menu_template);
        mTemplateItem.setTitle(mNote.isTemplate()? R.string.use_template : R.string.template);

        DrawableUtils.tintMenuWithAttribute(getActivity(), menu, R.attr.actionBarTextColor);

        super.onCreateOptionsMenu(menu, inflater);




    }

    private void cutContent(){
        String content = mNote.getContent();
        int newLinePosition = content.indexOf("\n");
        if (newLinePosition > 0)
            mContentEditText.setText(content.substring(0,newLinePosition));
    }

    private void refreshContent(){
            mContentEditText.setText(mNote.getContent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_todo:
                if (!isAdded()) return false;
                todoNote();
                return true;
            case R.id.menu_template:
                if (!isAdded()) return false;
                templateNote();
                return true;
            case R.id.menu_color:
                showColor();
                return true;
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
            case R.id.info_pin_switch_menu:
                item.setTitle(mNote.isPinned()? R.string.pin_to_top : R.string.unpin_from_top);
                NoteUtils.setNotePin(mNote, !mNote.isPinned());
                return true;
            case R.id.info_markdown_menu:
                item.setTitle(mNote.isMarkdownEnabled() ? R.string.markdown_show : R.string.markdown_hide);
                onInfoMarkdownSwitchChanged(!mNote.isMarkdownEnabled());
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

    private void templateNote() {
        if (mNote.isTemplate() == true) {
            copyNote(mNote);
        } else {
            NoteUtils.templateNote(mNote, getActivity());
            mTemplateItem.setTitle(R.string.use_template);
        }
    }

    private void todoNote() {
        if (mNote != null) {
            mIsTodo = mNote.isTodo();
            if(mIsTodo)  {
                mTodoComponent.setVisibility(View.GONE);
                mContentEditText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                mContentEditText.setSingleLine(false);

                mContentEditText.clearFocus();
                mContentEditText.setFocusableInTouchMode(false);
                mContentEditText.setFocusable(false);
                mContentEditText.setFocusableInTouchMode(true);
                mContentEditText.setFocusable(true);
                Toast.makeText(getContext(),
                        getContext().getString(R.string.note_from_todo), Toast.LENGTH_SHORT)
                        .show();
                mNote.setTodo(!mIsTodo);
                mNote.save();
            }
            else{
                String content = mNote.getContent();
                int newLinePosition = content.indexOf("\n");
                if (newLinePosition > 0)
                    showAlert(getContext().getString(R.string.loosing_note_content_message), getContext().getString(R.string.loosing_note_content));
                else{
                    mTodoComponent.setVisibility(View.VISIBLE);
                    mContentEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                    cutContent();
                    Toast.makeText(getContext(),
                            getContext().getString(R.string.todo_from_note), Toast.LENGTH_SHORT)
                            .show();
                    mNote.setTodo(!mIsTodo);
                    mNote.save();
                }
            }




        }
    }

    public void showAlert(String message, String title) {
        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(getContext());

        alertDialog2.setTitle(title);
        alertDialog2.setMessage(message);
        alertDialog2.setIcon(R.drawable.ic_action_remove_24dp);

        alertDialog2.setPositiveButton(getContext().getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mTodoComponent.setVisibility(View.VISIBLE);
                        mContentEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                        cutContent();
                        Toast.makeText(getContext(),
                                getContext().getString(R.string.todo_from_note), Toast.LENGTH_SHORT)
                                .show();
                        mNote.setTodo(!mIsTodo);
                        mNote.save();
                    }
                });

        alertDialog2.setNegativeButton(getContext().getString(R.string.no),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getContext(),
                            getContext().getString(R.string.still_note), Toast.LENGTH_SHORT)
                            .show();
                    dialog.cancel();
                }
            });
        alertDialog2.show();
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

    private void showColor() {
        if (mNote != null) {
            mContentEditText.clearFocus();
            saveNote();
            showColorPopUp();
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
                mReminderBottomSheet = new ReminderBottomSheetDialog(this, this);
            mReminderBottomSheet.show(mNote);
        }
    }

    private void showColorPopUp() {
        if (isAdded()) {
                mColorBottomSheet = new ColorBottomSheetDialog(this, this);
            mColorBottomSheet.show(mNote);
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

    public void setNote(String noteID) {
        setNote(noteID, null);
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
        AlarmUtils.createAlarm(getActivity(), mKey, mNote.getTitle(), mNote.getContentPreview(), mNote.getReminderDate());
        saveNote();
    }

    @Override
    public void onReminderOff() {
        mHasReminder = false;
        AlarmUtils.removeAlarm(getActivity(), mKey, mNote.getTitle(), mNote.getContentPreview());
        saveNote();
    }

    @Override
    public void onReminderUpdated(Calendar calendar) {
        mNote.setReminderDate(calendar);
        mHasReminderDateChange = true;
        mReminderBottomSheet.updateReminder(calendar);
        if (mHasReminder) {
            AlarmUtils.createAlarm(getActivity(), mKey, mNote.getTitle(), mNote.getContentPreview(), mNote.getReminderDate());
        }
    }

    @Override
    public void onReminderDismissed() {
        mReminderBottomSheet.dismiss();
    }

    protected void saveNote() {
        if (mNote == null || (mHistoryBottomSheet != null && mHistoryBottomSheet.isShowing())) {
            return;
        }

        String content = getNoteContentString();
        String tagString = getNoteTagsString();
        if (mHasReminderDateChange || mColor || mNote.hasChanges(content, tagString.trim(), mNote.isPinned(), mIsMarkdownEnabled, mHasReminder, mTodos, mTodosCompleted)) {
            mNote.setContent(content);
            mNote.setTagString(tagString);
            mNote.setModificationDate(Calendar.getInstance());
            mNote.setMarkdownEnabled(mIsMarkdownEnabled);
            mNote.setTodos(mTodos);
            mNote.setCompletedTodos(mTodosCompleted);

            mNote.save();

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_NOTE_EDITED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "editor_save"
            );
        }
    }

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
                mShareBottomSheet = new ShareBottomSheetDialog(this, this);
            mShareBottomSheet.show(mNote);
        }
    }

    private void showInfoSheet() {
        if (isAdded()) {
                mInfoBottomSheet = new InfoBottomSheetDialog(this, this);

            mInfoBottomSheet.show(mNote);
        }

    }

    private void showHistorySheet() {
        if (isAdded()) {

                mHistoryBottomSheet = new HistoryBottomSheetDialog(this, this);


            // Request revisions for the current note
            mNotesBucket.getRevisions(mNote, MAX_REVISIONS, mHistoryBottomSheet.getRevisionsRequestCallbacks());
            saveNote();

            mHistoryBottomSheet.show(mNote);
        }
    }

    public void onColorUpdate(int color) {
        mNote.setColor(color);
        mColor = true;
        mColorBottomSheet.updateColor(color);
        mColorIndicator.setBackgroundColor(color);

        if (color != Color.WHITE)
            mColorIndicator.setVisibility(View.VISIBLE);
    };

    public void onColorDismissed() {

    };


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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ReminderBottomSheetDialog.UPDATE_REMINDER_REQUEST_CODE) {
            long timestamp = data.getLongExtra(ReminderBottomSheetDialog.TIMESTAMP_BUNDLE_KEY, 0);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(timestamp));
            onReminderUpdated(calendar);
            mReminderBottomSheet.enableReminder();
        }
    }

    public class TodoItem {
        private String text;
        private Boolean checked;

        public TodoItem(String text, Boolean checked) {
            this.text = text;
            this.checked = checked;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Boolean getChecked() {
            return checked;
        }

        public void setChecked(Boolean checked) {
            this.checked = checked;
        }
    }

    public class JSONAdapter extends BaseAdapter implements ListAdapter {

        private final Activity activity;
        private final ArrayList<String> jsonArray;
        private final Boolean checked;

        public JSONAdapter (Activity activity, ArrayList<String> jsonArray, Boolean checked) {
            assert activity != null;
            assert jsonArray != null;

            this.jsonArray = jsonArray;
            this.activity = activity;
            this.checked = checked;
        }


        @Override public int getCount() {
            if(jsonArray == null)
                return 0;
            else
                return jsonArray.size();
        }

        @Override public String getItem(int position) {
            if (jsonArray == null)
                return null;
            else {
                return jsonArray.get(position);
            }
        }

        public void setItem(int position, String item) {
            if ((item != null) && (position < jsonArray.size())) {
                jsonArray.set(position, item);
            }
        }


        @Override public long getItemId(int position) {
            return position;
        }

        @Override public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = activity.getLayoutInflater().inflate(R.layout.fragment_todo_row, null);

            TextView text = (TextView)convertView.findViewById(R.id.todo_title);
            CheckBox check = (CheckBox)convertView.findViewById(R.id.todo_checked);
            View remove = (View)convertView.findViewById(R.id.todo_remove);

            String item = getItem(position);
            if(null != item ) {
                text.setText(item);
                check.setChecked(checked);
                if (checked)
                    text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }


            check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checked == false) {
                        String completed_item = mTodos.get(position);
                        mTodosCompleted.add(0, completed_item);
                        mTodos.remove(position);

                        mNote.setTodos(mTodos);
                        mNote.setCompletedTodos(mTodosCompleted);
                        mNote.save();
                        updateTodos();
                    } else {
                        String item = mTodosCompleted.get(position);
                        mTodos.add(0, item);
                        mTodosCompleted.remove(position);

                        mNote.setTodos(mTodos);
                        mNote.setCompletedTodos(mTodosCompleted);
                        mNote.save();
                        updateTodos();

                    }
                }
            });

            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checked == false) {
                        mTodos.remove(position);

                        mNote.setTodos(mTodos);
                        mNote.setCompletedTodos(mTodosCompleted);
                        mNote.save();
                        updateTodos();
                    } else {
                        String item = mTodosCompleted.get(position);
                        mTodosCompleted.remove(item);

                        mNote.setTodos(mTodos);
                        mNote.setCompletedTodos(mTodosCompleted);
                        mNote.save();
                        updateTodos();
                    }
                }
            });


            final JSONAdapter jSONAdapter = this;

            text.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (checked == false) {
                        mTodos.set(position, charSequence.toString());
                        mNote.setTodos(mTodos);
                    } else {
                        mTodosCompleted.set(position, charSequence.toString());
                        mNote.setCompletedTodos(mTodosCompleted);
                    }

                    NoteUtils.setListViewHeight(mTodoList);
                    NoteUtils.setListViewHeight(mCompletedTodoList);

                    mNote.save();
                }

                @Override
                public void afterTextChanged(Editable editable) {}
            });

            return convertView;
        }
    }

    public void updateReminder(Calendar aCalendar) {
        onReminderUpdated(aCalendar);
    }

    @Override
    public void onColorChanged(int tempcolor, String paletteId, String colorName, String paletteName) {
        mNote.setColor(tempcolor);
        mColor = true;
        mColorBottomSheet.updateColor(tempcolor);
        mColorIndicator.setBackgroundColor(tempcolor);
    }

    @Override
    public void onColorDialogCancelled() {

    }

    public void copyNote(Note source) {

        // Create & save new note
        Simplenote simplenote = (Simplenote) getActivity().getApplication();
        Bucket<Note> notesBucket = simplenote.getNotesBucket();
        Note note = notesBucket.newObject();
        note.setCreationDate(Calendar.getInstance());
        note.setModificationDate(note.getCreationDate());
        note.setMarkdownEnabled(source.isMarkdownEnabled());
        note.setContent(source.getContent());
        note.setColor(source.getColor());
        note.setTags(source.getTags());
        note.setPinned(source.isPinned());
        note.setTodo(source.isTodo());
        note.setTodos(source.getTodos());
        note.setCompletedTodos(source.getCompletedTodos());

        note.save();

        Bundle arguments = new Bundle();
        arguments.putString(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
        arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, true);
        arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());
        Intent editNoteIntent = new Intent(getActivity(), NoteEditorActivity.class);
        editNoteIntent.putExtras(arguments);

        getActivity().startActivityForResult(editNoteIntent, Simplenote.INTENT_EDIT_NOTE);

    }

    private void updateTodos() {
        jSONAdapter = new JSONAdapter(this.getActivity(), mNote.getTodos(), false);
        mTodoList.setAdapter(jSONAdapter);
        jSONAdapter = new JSONAdapter(this.getActivity(), mNote.getCompletedTodos(), true);
        mCompletedTodoList.setAdapter(jSONAdapter);

        if ((mNote.getTodos().size() == 0) || (mNote.getCompletedTodos().size() == 0) )
            mTodoDivider.setVisibility(View.GONE);
        else
            mTodoDivider.setVisibility(View.VISIBLE);

        NoteUtils.setListViewHeight(mTodoList);
        NoteUtils.setListViewHeight(mCompletedTodoList);
    }

}