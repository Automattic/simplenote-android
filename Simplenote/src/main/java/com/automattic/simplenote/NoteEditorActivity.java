package com.automattic.simplenote;

import static androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;
import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_WIDGET;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_LIST_WIDGET_NOTE_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_NOTE_TAPPED;
import static com.automattic.simplenote.utils.DisplayUtils.disableScreenshotsIfLocked;
import static com.automattic.simplenote.utils.MatchOffsetHighlighter.MATCH_INDEX_COUNT;
import static com.automattic.simplenote.utils.MatchOffsetHighlighter.MATCH_INDEX_START;
import static com.automattic.simplenote.utils.WidgetUtils.KEY_LIST_WIDGET_CLICK;
import static com.automattic.simplenote.utils.WidgetUtils.KEY_WIDGET_CLICK;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.AppLog.Type;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.NetworkUtils;
import com.automattic.simplenote.widgets.NoteEditorViewPager;
import com.automattic.simplenote.widgets.RobotoMediumTextView;
import com.google.android.material.tabs.TabLayout;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class NoteEditorActivity extends ThemedAppCompatActivity {
    private static final String STATE_TAB_EDIT = "TAB_EDIT";
    private static final String STATE_TAB_PREVIEW = "TAB_PREVIEW";
    private static final String STATE_MATCHES_INDEX = "MATCHES_INDEX";
    private static final String STATE_MATCHES_LOCATIONS = "MATCHES_LOCATIONS";
    private static final int INDEX_TAB_EDIT = 0;
    private static final int INDEX_TAB_PREVIEW = 1;

    private ImageButton mButtonPrevious;
    private ImageButton mButtonNext;
    private Note mNote;
    private NoteEditorFragment mNoteEditorFragment;
    private NoteEditorFragmentPagerAdapter mNoteEditorFragmentPagerAdapter;
    private NoteEditorViewPager mViewPager;
    private RelativeLayout mSearchMatchBar;
    private RobotoMediumTextView mNumberPosition;
    private RobotoMediumTextView mNumberTotal;
    private String mNoteId;
    private TabLayout mTabLayout;
    private boolean isMarkdownEnabled;
    private boolean isPreviewEnabled;
    private boolean isSearchMatch;
    private int[] mSearchMatchIndexes;
    private int mSearchMatchIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppLog.add(Type.NETWORK, NetworkUtils.getNetworkInfo(NoteEditorActivity.this));
        AppLog.add(Type.SCREEN, "Created (NoteEditorActivity)");
        setContentView(R.layout.activity_note_editor);

        // No title, please.
        setTitle("");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Add a custom handler for back button click
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleBackPressed();
                }
            });
        }

        mNoteEditorFragment = new NoteEditorFragment();
        NoteMarkdownFragment noteMarkdownFragment;

        mNoteEditorFragmentPagerAdapter = new NoteEditorFragmentPagerAdapter(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mViewPager = findViewById(R.id.pager);
        mTabLayout = findViewById(R.id.tabs);

        Intent intent = getIntent();
        mNoteId = intent.getStringExtra(NoteEditorFragment.ARG_ITEM_ID);

        try {
            Simplenote application = (Simplenote) getApplication();
            Bucket<Note> notesBucket = application.getNotesBucket();
            mNote = notesBucket.get(mNoteId);
        } catch (BucketObjectMissingException exception) {
            exception.printStackTrace();
        }

        if (savedInstanceState == null) {
            // Create the note editor fragment
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, mNoteId);
            arguments.putBoolean(NoteEditorFragment.ARG_IS_FROM_WIDGET,
                    intent.getBooleanExtra(NoteEditorFragment.ARG_IS_FROM_WIDGET, false));

            boolean isNewNote = intent.getBooleanExtra(NoteEditorFragment.ARG_NEW_NOTE, false);
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, isNewNote);
            if (intent.hasExtra(NoteEditorFragment.ARG_MATCH_OFFSETS))
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS,
                        intent.getStringExtra(NoteEditorFragment.ARG_MATCH_OFFSETS));

            mNoteEditorFragment.setArguments(arguments);
            noteMarkdownFragment = new NoteMarkdownFragment();
            noteMarkdownFragment.setArguments(arguments);

            mNoteEditorFragmentPagerAdapter.addFragment(
                    mNoteEditorFragment,
                    getString(R.string.tab_edit)
            );
            mNoteEditorFragmentPagerAdapter.addFragment(
                    noteMarkdownFragment,
                    getString(R.string.tab_preview)
            );
            mViewPager.setPagingEnabled(false);
            mViewPager.addOnPageChangeListener(
                new NoteEditorViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        if (position == INDEX_TAB_PREVIEW) {
                            DisplayUtils.hideKeyboard(mViewPager);
                        }

                        try {
                            Simplenote application = (Simplenote) getApplication();
                            Bucket<Note> notesBucket = application.getNotesBucket();
                            mNote = notesBucket.get(mNoteId);

                            if (mNote != null) {
                                mNote.setPreviewEnabled(position == INDEX_TAB_PREVIEW);
                                mNote.save();
                            }
                        } catch (BucketObjectMissingException exception) {
                            exception.printStackTrace();
                        }
                    }

                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }
                }
            );

            isMarkdownEnabled = intent.getBooleanExtra(NoteEditorFragment.ARG_MARKDOWN_ENABLED, false);
            isPreviewEnabled = intent.getBooleanExtra(NoteEditorFragment.ARG_PREVIEW_ENABLED, false);
        } else {
            mNoteEditorFragmentPagerAdapter.addFragment(
                    getSupportFragmentManager().getFragment(savedInstanceState, STATE_TAB_EDIT),
                    getString(R.string.tab_edit)
            );
            mNoteEditorFragmentPagerAdapter.addFragment(
                    getSupportFragmentManager().getFragment(savedInstanceState, STATE_TAB_PREVIEW),
                    getString(R.string.tab_preview)
            );

            isMarkdownEnabled = savedInstanceState.getBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED);
            isPreviewEnabled = savedInstanceState.getBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED);
            mSearchMatchIndex = savedInstanceState.getInt(STATE_MATCHES_INDEX, 0);
            mSearchMatchIndexes = savedInstanceState.getIntArray(STATE_MATCHES_LOCATIONS);
        }

        if (intent.hasExtra(NoteEditorFragment.ARG_MATCH_OFFSETS)) {
            setUpSearchMatchBar(intent);
            isSearchMatch = true;
        }

        mViewPager.setAdapter(mNoteEditorFragmentPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        // Show tabs if markdown is enabled for the current note.
        if (isMarkdownEnabled) {
            showTabs();

            if (isPreviewEnabled & !isSearchMatch) {
                mViewPager.setCurrentItem(mNoteEditorFragmentPagerAdapter.getCount() - 1);
            }
        }

        if (intent.hasExtra(KEY_WIDGET_CLICK) && intent.getExtras() != null &&
            intent.getExtras().getSerializable(KEY_WIDGET_CLICK) == NOTE_WIDGET_NOTE_TAPPED) {
            AnalyticsTracker.track(
                NOTE_WIDGET_NOTE_TAPPED,
                CATEGORY_WIDGET,
                "note_widget_note_tapped"
            );
        }

        if (intent.hasExtra(KEY_LIST_WIDGET_CLICK) && intent.getExtras() != null &&
            intent.getExtras().getSerializable(KEY_LIST_WIDGET_CLICK) == NOTE_LIST_WIDGET_NOTE_TAPPED) {
            AnalyticsTracker.track(
                NOTE_LIST_WIDGET_NOTE_TAPPED,
                CATEGORY_WIDGET,
                "note_list_widget_note_tapped"
            );
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    private void handleBackPressed() {
        AppLog.add(Type.ACTION, "Tapped back button in navigation bar (NoteEditorActivity)");
        if (isTaskRoot()) {
            // The editor can be the task root when it comes from an action on a widget
            // In these cases, instead of going to the home screen, the notes activity
            // is started
            Intent intent = new Intent(getApplicationContext(), NotesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getAppLock().setExemptActivities(null);
        }

        AppLog.add(Type.SCREEN, "Paused (NoteEditorActivity)");
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableScreenshotsIfLocked(this);
        AppLog.add(Type.NETWORK, NetworkUtils.getNetworkInfo(NoteEditorActivity.this));
        AppLog.add(Type.SCREEN, "Resumed (NoteEditorActivity)");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mNoteEditorFragmentPagerAdapter.getCount() > 0 && mNoteEditorFragmentPagerAdapter.getItem(INDEX_TAB_EDIT).isAdded()) {
            getSupportFragmentManager().putFragment(outState, STATE_TAB_EDIT, mNoteEditorFragmentPagerAdapter.getItem(INDEX_TAB_EDIT));
        }

        if (mNoteEditorFragmentPagerAdapter.getCount() > 1 && mNoteEditorFragmentPagerAdapter.getItem(INDEX_TAB_PREVIEW).isAdded()) {
            getSupportFragmentManager().putFragment(outState, STATE_TAB_PREVIEW, mNoteEditorFragmentPagerAdapter.getItem(INDEX_TAB_PREVIEW));
        }

        outState.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, isMarkdownEnabled);
        outState.putBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED, isPreviewEnabled);
        outState.putInt(STATE_MATCHES_INDEX, mSearchMatchIndex);
        outState.putIntArray(STATE_MATCHES_LOCATIONS, mSearchMatchIndexes);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Relaunch shortcut dialog when window is maximized or restored (Chrome OS).
        if (getSupportFragmentManager().findFragmentByTag(ShortcutDialogFragment.DIALOG_TAG) != null) {
            ShortcutDialogFragment.showShortcuts(NoteEditorActivity.this, isPreviewTabSelected());
        }

        // If changing to large screen landscape, we finish the activity to go back to
        // NotesActivity with the note selected in the multipane layout.
        if (DisplayUtils.isLargeScreen(this) &&
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && mNoteId != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(Simplenote.SELECTED_NOTE_ID, mNoteId);
            resultIntent.putExtra(NoteEditorFragment.ARG_PREVIEW_ENABLED, isPreviewEnabled);
            resultIntent.putExtra(NoteEditorFragment.ARG_MARKDOWN_ENABLED, isMarkdownEnabled);
            resultIntent.putExtra(ShortcutDialogFragment.DIALOG_VISIBLE,
                    getSupportFragmentManager().findFragmentByTag(ShortcutDialogFragment.DIALOG_TAG) != null);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_C:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (!isPreviewTabSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.insertChecklist();
                        }
                    } else {
                        Toast.makeText(NoteEditorActivity.this, R.string.item_action_toggle_checklist_edit_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_COMMA:
                if (event.isCtrlPressed()) {
                    ShortcutDialogFragment.showShortcuts(NoteEditorActivity.this, isPreviewTabSelected());
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_H:
                if (event.isCtrlPressed()) {
                    if (!isPreviewTabSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.showHistory();
                        }
                    } else {
                        Toast.makeText(NoteEditorActivity.this, R.string.item_action_show_history_edit_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_I:
                if (event.isCtrlPressed()) {
                    if (!isPreviewTabSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.showInfo();
                        }
                    } else {
                        Toast.makeText(NoteEditorActivity.this, R.string.item_action_show_information_edit_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_P:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (mNote != null && mNote.isMarkdownEnabled()) {
                        togglePreview();
                    } else {
                        Toast.makeText(NoteEditorActivity.this, R.string.item_action_toggle_preview_enable_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_S:
                if (event.isCtrlPressed()) {
                    if (!isPreviewTabSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.shareNote();
                        }
                    } else {
                        Toast.makeText(NoteEditorActivity.this, R.string.item_action_show_share_edit_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    protected NoteMarkdownFragment getNoteMarkdownFragment() {
        return (NoteMarkdownFragment) mNoteEditorFragmentPagerAdapter.getItem(1);
    }

    public void hideTabs() {
        mTabLayout.setVisibility(View.GONE);
        mViewPager.setPagingEnabled(false);
    }

    private boolean isPreviewTabSelected() {
        return mNote != null && mNote.isMarkdownEnabled() && mViewPager != null && mViewPager.getCurrentItem() == INDEX_TAB_PREVIEW;
    }

    public void showTabs() {
        mTabLayout.setVisibility(View.VISIBLE);
        mViewPager.setPagingEnabled(true);
    }

    public void setSearchMatchBarVisible(boolean isVisible) {
        if (mSearchMatchBar != null) {
            mSearchMatchBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void setUpSearchMatchBar(Intent intent) {
        if (mSearchMatchIndexes == null) {
            String matchOffsets = intent.getStringExtra(NoteEditorFragment.ARG_MATCH_OFFSETS);
            String[] matches = matchOffsets != null ? matchOffsets.split("\\s+") : new String[]{};
            String[] matchesStart = new String[matches.length / MATCH_INDEX_COUNT];

            // Get "start" item from matches.  The format is four space-separated integers that
            // represent the location of the match: "column token start length" ex: "1 3 3 7"
            for (int i = MATCH_INDEX_START, j = 0; i < matches.length; i += MATCH_INDEX_COUNT, j++) {
                matchesStart[j] = matches[i];
            }

            // Remove duplicate items with linked hash set and linked list since full-text search
            // may return the same position more than once when parsing both title and content.
            matchesStart = new LinkedHashSet<>(new LinkedList<>(Arrays.asList(matchesStart))).toArray(new String[0]);
            mSearchMatchIndexes = new int[matchesStart.length];

            // Convert matches string array to integer array.
            for (int i = 0; i < matchesStart.length; i++) {
                mSearchMatchIndexes[i] = Integer.parseInt(matchesStart[i]);
            }
        }

        mSearchMatchBar = findViewById(R.id.search_match_bar);
        mNumberPosition = findViewById(R.id.text_position);
        mNumberTotal = findViewById(R.id.text_total);
        mButtonPrevious = findViewById(R.id.button_previous);
        mButtonNext = findViewById(R.id.button_next);

        mButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSearchMatchIndex > 0) {
                    AnalyticsTracker.track(
                        AnalyticsTracker.Stat.SEARCH_MATCH_TAPPED,
                        AnalyticsTracker.CATEGORY_SEARCH,
                        "search_match_tapped_previous"
                    );
                    mSearchMatchIndex--;
                    mNoteEditorFragment.scrollToMatch(mSearchMatchIndexes[mSearchMatchIndex]);
                    new Handler().postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                updateSearchMatchBarStatus();
                            }
                        },
                        getResources().getInteger(android.R.integer.config_mediumAnimTime)
                    );
                }
            }
        });
        mButtonPrevious.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v.isHapticFeedbackEnabled()) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(NoteEditorActivity.this, getString(R.string.previous), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSearchMatchIndex < mSearchMatchIndexes.length - 1) {
                    AnalyticsTracker.track(
                        AnalyticsTracker.Stat.SEARCH_MATCH_TAPPED,
                        AnalyticsTracker.CATEGORY_SEARCH,
                        "search_match_tapped_next"
                    );
                    mSearchMatchIndex++;
                    mNoteEditorFragment.scrollToMatch(mSearchMatchIndexes[mSearchMatchIndex]);
                    new Handler().postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                updateSearchMatchBarStatus();
                            }
                        },
                        getResources().getInteger(android.R.integer.config_mediumAnimTime)
                    );
                }
            }
        });
        mButtonNext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v.isHapticFeedbackEnabled()) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(NoteEditorActivity.this, getString(R.string.next), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mNoteEditorFragment.scrollToMatch(mSearchMatchIndexes[mSearchMatchIndex]);
        setSearchMatchBarVisible(true);
        updateSearchMatchBarStatus();
    }

    private void togglePreview() {
        int position = mNote.isPreviewEnabled() ? INDEX_TAB_EDIT : INDEX_TAB_PREVIEW;
        mViewPager.setCurrentItem(position);

        try {
            Simplenote application = (Simplenote) getApplication();
            Bucket<Note> notesBucket = application.getNotesBucket();
            mNote = notesBucket.get(mNoteId);

            if (mNote != null) {
                mNote.setPreviewEnabled(position == INDEX_TAB_PREVIEW);
                mNote.save();
            }
        } catch (BucketObjectMissingException exception) {
            exception.printStackTrace();
        }
    }

    private void updateSearchMatchBarStatus() {
        mNumberPosition.setText(String.valueOf(mSearchMatchIndex + 1));
        mNumberTotal.setText(String.valueOf(mSearchMatchIndexes.length));
        mButtonPrevious.setEnabled(mSearchMatchIndex > 0);
        mButtonNext.setEnabled(mSearchMatchIndex < mSearchMatchIndexes.length - 1);
    }

    private static class NoteEditorFragmentPagerAdapter extends FragmentPagerAdapter {
        private final ArrayList<Fragment> mFragments = new ArrayList<>();
        private final ArrayList<String> mTitles = new ArrayList<>();

        NoteEditorFragmentPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles.get(position);
        }

        void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mTitles.add(title);
            notifyDataSetChanged();
        }
    }
}
