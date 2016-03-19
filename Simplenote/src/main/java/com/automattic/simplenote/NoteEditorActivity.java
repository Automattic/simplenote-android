package com.automattic.simplenote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.NoteEditorViewPager;

import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;

public class NoteEditorActivity extends AppCompatActivity {
    private TabLayout mTabLayout;
    private NoteEditorFragmentPagerAdapter mNoteEditorFragmentPagerAdapter;
    private NoteEditorViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_editor);

        // No title, please.
        setTitle("");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        NoteEditorFragment noteEditorFragment;
        NoteMarkdownFragment noteMarkdownFragment;

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            // Create the note editor fragment
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID,
                    intent.getStringExtra(NoteEditorFragment.ARG_ITEM_ID));

            boolean isNewNote = intent.getBooleanExtra(NoteEditorFragment.ARG_NEW_NOTE, false);
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, isNewNote);
            if (intent.hasExtra(NoteEditorFragment.ARG_MATCH_OFFSETS))
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS,
                    intent.getStringExtra(NoteEditorFragment.ARG_MATCH_OFFSETS));

            noteEditorFragment = new NoteEditorFragment();
            noteEditorFragment.setArguments(arguments);
            noteMarkdownFragment = new NoteMarkdownFragment();
            noteMarkdownFragment.setArguments(arguments);

            mViewPager = (NoteEditorViewPager) findViewById(R.id.pager);
            mNoteEditorFragmentPagerAdapter =
                    new NoteEditorFragmentPagerAdapter(getSupportFragmentManager());
            mNoteEditorFragmentPagerAdapter.addFragment(
                    noteEditorFragment,
                    getString(R.string.tab_edit)
            );
            mNoteEditorFragmentPagerAdapter.addFragment(
                    noteMarkdownFragment,
                    getString(R.string.tab_preview)
            );
            mViewPager.setAdapter(mNoteEditorFragmentPagerAdapter);
            mViewPager.setPagingEnabled(false);
            mViewPager.addOnPageChangeListener(
                new NoteEditorViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        if (position == 1) {
                            final InputMethodManager imm = (InputMethodManager) getSystemService(
                                    Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
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

            mTabLayout = (TabLayout) findViewById(R.id.tabs);
            mTabLayout.setupWithViewPager(mViewPager);

            // Show tabs if markdown is enabled globally and for current note.
            if (PrefUtils.getBoolPref(NoteEditorActivity.this, PrefUtils.PREF_MARKDOWN_ENABLED, false)
                    && intent.getBooleanExtra(NoteEditorFragment.ARG_MARKDOWN_ENABLED, false)) {
                showTabs();
            }
        }
    }

    @Override
    protected void onPause() {
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getCurrentAppLock().setDisabledActivities(null);
        }

        super.onPause();
    }

    protected NoteMarkdownFragment getNoteMarkdownFragment() {
        return (NoteMarkdownFragment) mNoteEditorFragmentPagerAdapter.getFragment(1);
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

        public NoteEditorFragmentPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles.get(position);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mTitles.add(title);
            notifyDataSetChanged();
        }

        public Fragment getFragment(int position) {
            return mFragments.get(position);
        }
    }
}
