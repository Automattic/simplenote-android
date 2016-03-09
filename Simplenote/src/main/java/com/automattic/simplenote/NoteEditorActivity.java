package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.NoteEditorViewPager;

import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;

public class NoteEditorActivity extends AppCompatActivity {
    private TabLayout mTabLayout;
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

            mViewPager = (NoteEditorViewPager) findViewById(R.id.pager);
            NoteEditorFragmentStatePagerAdapter noteEditorFragmentStatePagerAdapter =
                    new NoteEditorFragmentStatePagerAdapter(getSupportFragmentManager());
            noteEditorFragmentStatePagerAdapter.addFragment(
                    noteEditorFragment,
                    getString(R.string.tab_edit)
            );
            noteEditorFragmentStatePagerAdapter.addFragment(
                    new NoteMarkdownFragment(),
                    getString(R.string.tab_preview)
            );
            mViewPager.setAdapter(noteEditorFragmentStatePagerAdapter);
            mViewPager.setPagingEnabled(false);

            mTabLayout = (TabLayout) findViewById(R.id.tabs);
            mTabLayout.setupWithViewPager(mViewPager);
        }
    }

    @Override
    protected void onPause() {
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getCurrentAppLock().setDisabledActivities(null);
        }

        super.onPause();
    }

    public void hideTabs() {
        mTabLayout.setVisibility(View.GONE);
        mViewPager.setPagingEnabled(false);
    }

    public void showTabs() {
        mTabLayout.setVisibility(View.VISIBLE);
        mViewPager.setPagingEnabled(true);
    }

    private static class NoteEditorFragmentStatePagerAdapter extends FragmentStatePagerAdapter {
        private final ArrayList<Fragment> mFragments = new ArrayList<>();
        private final ArrayList<String> mTitles = new ArrayList<>();

        public NoteEditorFragmentStatePagerAdapter(FragmentManager manager) {
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
    }
}
