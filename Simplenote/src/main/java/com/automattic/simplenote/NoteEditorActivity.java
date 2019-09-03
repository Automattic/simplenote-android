package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.NoteEditorViewPager;
import com.google.android.material.tabs.TabLayout;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;

import static com.automattic.simplenote.NoteWidget.KEY_WIDGET_CLICK;
import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_WIDGET;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_NOTE_TAPPED;
import static com.automattic.simplenote.utils.DisplayUtils.disableScreenshotsIfLocked;

public class NoteEditorActivity extends AppCompatActivity {
    private TabLayout mTabLayout;
    private Note mNote;
    private NoteEditorFragmentPagerAdapter mNoteEditorFragmentPagerAdapter;
    private NoteEditorViewPager mViewPager;
    private boolean isMarkdownEnabled;
    private boolean isPreviewEnabled;
    private String mNoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_editor);

        // No title, please.
        setTitle("");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        NoteEditorFragment noteEditorFragment;
        NoteMarkdownFragment noteMarkdownFragment;

        mNoteEditorFragmentPagerAdapter =
                new NoteEditorFragmentPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.pager);
        mTabLayout = findViewById(R.id.tabs);

        Intent intent = getIntent();
        mNoteId = intent.getStringExtra(NoteEditorFragment.ARG_ITEM_ID);

        if (savedInstanceState == null) {
            // Create the note editor fragment
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, mNoteId);

            boolean isNewNote = intent.getBooleanExtra(NoteEditorFragment.ARG_NEW_NOTE, false);
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, isNewNote);
            if (intent.hasExtra(NoteEditorFragment.ARG_MATCH_OFFSETS))
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS,
                        intent.getStringExtra(NoteEditorFragment.ARG_MATCH_OFFSETS));

            noteEditorFragment = new NoteEditorFragment();
            noteEditorFragment.setArguments(arguments);
            noteMarkdownFragment = new NoteMarkdownFragment();
            noteMarkdownFragment.setArguments(arguments);

            mNoteEditorFragmentPagerAdapter.addFragment(
                    noteEditorFragment,
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
                            if (position == 1) {  // Preview is position 1
                                DisplayUtils.hideKeyboard(mViewPager);
                            }

                            try {
                                Simplenote application = (Simplenote) getApplication();
                                Bucket<Note> notesBucket = application.getNotesBucket();
                                mNote = notesBucket.get(mNoteId);

                                if (mNote != null) {
                                    mNote.setPreviewEnabled(position == 1);  // Preview is position 1
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
                    getSupportFragmentManager().getFragment(savedInstanceState, getString(R.string.tab_edit)),
                    getString(R.string.tab_edit)
            );
            mNoteEditorFragmentPagerAdapter.addFragment(
                    getSupportFragmentManager().getFragment(savedInstanceState, getString(R.string.tab_preview)),
                    getString(R.string.tab_preview)
            );

            isMarkdownEnabled = savedInstanceState.getBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED);
            isPreviewEnabled = savedInstanceState.getBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED);
        }

        mViewPager.setAdapter(mNoteEditorFragmentPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        // Show tabs if markdown is enabled for the current note.
        if (isMarkdownEnabled) {
            showTabs();

            if (isPreviewEnabled) {
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getAppLock().setExemptActivities(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableScreenshotsIfLocked(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mNoteEditorFragmentPagerAdapter.getCount() > 0 && mNoteEditorFragmentPagerAdapter.getItem(0).isAdded()) {
            getSupportFragmentManager()
                    .putFragment(outState, getString(R.string.tab_edit), mNoteEditorFragmentPagerAdapter.getItem(0));
        }
        if (mNoteEditorFragmentPagerAdapter.getCount() > 1 && mNoteEditorFragmentPagerAdapter.getItem(1).isAdded()) {
            getSupportFragmentManager()
                    .putFragment(outState, getString(R.string.tab_preview), mNoteEditorFragmentPagerAdapter.getItem(1));
        }
        outState.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, isMarkdownEnabled);
        outState.putBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED, isPreviewEnabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // If changing to large screen landscape, we finish the activity to go back to
        // NotesActivity with the note selected in the multipane layout.
        if (DisplayUtils.isLargeScreen(this) &&
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && mNoteId != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(Simplenote.SELECTED_NOTE_ID, mNoteId);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    protected NoteMarkdownFragment getNoteMarkdownFragment() {
        return (NoteMarkdownFragment) mNoteEditorFragmentPagerAdapter.getItem(1);
    }

    public void hideTabs() {
        mTabLayout.setVisibility(View.GONE);
        mViewPager.setPagingEnabled(false);
    }

    public void showTabs() {
        mTabLayout.setVisibility(View.VISIBLE);
        mViewPager.setPagingEnabled(true);
    }

    private static class NoteEditorFragmentPagerAdapter extends FragmentPagerAdapter {
        private final ArrayList<Fragment> mFragments = new ArrayList<>();
        private final ArrayList<String> mTitles = new ArrayList<>();

        NoteEditorFragmentPagerAdapter(FragmentManager manager) {
            super(manager);
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
