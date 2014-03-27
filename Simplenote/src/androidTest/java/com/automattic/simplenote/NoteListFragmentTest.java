package com.automattic.simplenote;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.NoteCountIndexer;
import com.automattic.simplenote.models.NoteTagger;
import com.automattic.simplenote.models.Tag;

import android.test.ActivityInstrumentationTestCase2;

import com.automattic.simplenote.simperium.MockAndroidClient;

import com.simperium.Simperium;
import com.simperium.client.Bucket;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.automattic.simplenote.NotesActivityTest \
 * com.automattic.simplenote.tests/android.test.InstrumentationTestRunner
 */
public class NoteListFragmentTest extends ActivityInstrumentationTestCase2<NotesActivity> {

    Simperium mSimperium;
    MockAndroidClient mClient;
    NotesActivity mActivity;
    Bucket<Note> mNotesBucket;
    Bucket<Tag> mTagsBucket;

    public NoteListFragmentTest() {
        super(NotesActivity.class);
    }

    @Override
    protected void setUp() throws Exception {

        super.setUp();

        mActivity = getActivity();

        mClient = new MockAndroidClient(mActivity);

        mSimperium = new Simperium("mock-app", "mock-secret", mClient);

        mNotesBucket = mSimperium.bucket(new Note.Schema());
        Tag.Schema tagSchema = new Tag.Schema();
        tagSchema.addIndex(new NoteCountIndexer(mNotesBucket));
        mTagsBucket = mSimperium.bucket(tagSchema);

        // Every time a note changes or is deleted we need to reindex the tag counts
        mNotesBucket.addListener(new NoteTagger(mTagsBucket));

        mActivity.mNotesBucket = mNotesBucket;
        mActivity.mTagsBucket = mTagsBucket;

    }

    /**
     * Test to reproduce issue #142, issues refreshList async task a cursor then sets
     * a search string so when the cursor returns the NoteCursorAdapter attempts to
     * access the <code>match_offset</code> field.
     * 
     * See: https://github.com/Simperium/simplenote-android/issues/142
     *
     * Fails on Android 4.0.3 (android-15) amd emulator
     */
    public void testNonSearchCursorReturnsAfterSearchApplied()
    throws Exception {

        NotesActivity activity = getActivity();

        NoteListFragment noteListFragment = activity.getNoteListFragment();

        assertNotNull(noteListFragment);
        noteListFragment.refreshList();

        NoteListFragment.NotesCursorAdapter adapter = noteListFragment.mNotesAdapter;
        noteListFragment.mSearchString = "welcome";

        assertEquals(1, adapter.getCount());

    }

}
