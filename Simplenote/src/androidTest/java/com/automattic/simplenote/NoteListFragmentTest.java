package com.automattic.simplenote;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NoteListFragmentTest {
    @Rule
    public ActivityTestRule<NotesActivity> mActivityRule = new ActivityTestRule<>(NotesActivity.class);
    private NotesActivity mActivity;

    @Before
    public void setUp() throws Exception {
        mActivity = mActivityRule.getActivity();
    }

    /**
     * Test to reproduce issue #142, issues refreshList async task a cursor then sets
     * a search string so when the cursor returns the NoteCursorAdapter attempts to
     * access the <code>match_offset</code> field.
     * <p>
     * See: https://github.com/Simperium/simplenote-android/issues/142
     * <p>
     * Fails on Android 4.0.3 (android-15) amd emulator
     */
    @Test
    public void testNonSearchCursorReturnsAfterSearchApplied() {
        NoteListFragment noteListFragment = mActivity.getNoteListFragment();

        assertThat(noteListFragment, not(nullValue()));
        noteListFragment.refreshList();

        NoteListFragment.NotesCursorAdapter adapter = noteListFragment.mNotesAdapter;
        noteListFragment.mSearchString = "welcome";

        assertThat(adapter.getCount(), is(1));

    }
}
