package com.automattic.simplenote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.widgets.SimplenoteEditText;
import com.simperium.client.Bucket;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NoteEditorFragmentTest {
    @Rule
    public ActivityTestRule<NoteEditorActivity> mActivityRule =
            new ActivityTestRule<>(NoteEditorActivity.class, true, false);

    private NoteEditorFragment mAttachedFragment;

    @After
    public void tearDown() {
        if (mAttachedFragment != null) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                mAttachedFragment.cancelPendingSavesForTest();
            });
        }
    }

    @Test
    @SmallTest
    public void testSetTitleSpanSingleLine() {
        SpannableStringBuilder text = new SpannableStringBuilder("Single line title");
        NoteEditorFragment.setTitleSpan(text);

        RelativeSizeSpan[] sizeSpans = text.getSpans(0, text.length(), RelativeSizeSpan.class);
        assertEquals(1, sizeSpans.length);
        assertEquals(0, text.getSpanStart(sizeSpans[0]));
        assertEquals(text.length(), text.getSpanEnd(sizeSpans[0]));
        assertEquals(1.3f, sizeSpans[0].getSizeChange(), 0.01f);

        StyleSpan[] styleSpans = text.getSpans(0, text.length(), StyleSpan.class);
        boolean foundBold = false;
        for (StyleSpan span : styleSpans) {
            if (span.getStyle() == Typeface.BOLD) {
                foundBold = true;
                assertEquals(0, text.getSpanStart(span));
                assertEquals(text.length(), text.getSpanEnd(span));
            }
        }
        assertTrue("Title should have bold style span", foundBold);
    }

    @Test
    @SmallTest
    public void testSetTitleSpanEmptyFirstLine() {
        SpannableStringBuilder text = new SpannableStringBuilder("\nSecond line\nThird line");
        NoteEditorFragment.setTitleSpan(text);

        RelativeSizeSpan[] sizeSpans = text.getSpans(0, text.length(), RelativeSizeSpan.class);
        assertEquals("No RelativeSizeSpan should exist when first line is empty", 0, sizeSpans.length);

        StyleSpan[] styleSpans = text.getSpans(0, text.length(), StyleSpan.class);
        for (StyleSpan span : styleSpans) {
            if (span.getStyle() == Typeface.BOLD) {
                org.junit.Assert.fail("No bold StyleSpan should exist when first line is empty");
            }
        }
    }

    @Test
    @SmallTest
    public void testSetTitleSpanDynamicUpdate() {
        SpannableStringBuilder text = new SpannableStringBuilder("Initial Long Title\nLine 2");
        NoteEditorFragment.setTitleSpan(text);

        RelativeSizeSpan[] initialSpans = text.getSpans(0, text.length(), RelativeSizeSpan.class);
        assertEquals(1, initialSpans.length);
        assertEquals("Initial Long Title".length(), text.getSpanEnd(initialSpans[0]));

        // Update title to shorter text
        text.replace(0, "Initial Long Title".length(), "Short");
        NoteEditorFragment.setTitleSpan(text);

        RelativeSizeSpan[] updatedSpans = text.getSpans(0, text.length(), RelativeSizeSpan.class);
        assertEquals(1, updatedSpans.length);
        assertEquals("Short".length(), text.getSpanEnd(updatedSpans[0]));
    }

    @Test
    @SmallTest
    public void testSaveScrollPositionIsolatedAndOnDestroyView() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences("test_scroll_prefs", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();

        NoteEditorFragment fragment = new NoteEditorFragment();
        fragment.setPreferencesForTest(preferences);

        Note note = new Note("test_note_key_1");
        fragment.setNoteForTest(note);

        SimplenoteEditText editText = new SimplenoteEditText(context);
        editText.scrollTo(0, 250);
        fragment.setContentEditTextForTest(editText);

        fragment.saveScrollPosition();
        assertEquals(250, preferences.getInt("test_note_key_1", -1));

        // Test onDestroyView
        editText.scrollTo(0, 400);
        fragment.onDestroyView();
        assertEquals(400, preferences.getInt("test_note_key_1", -1));
    }

    @Test
    @SmallTest
    public void testScrollPersistenceNullGuards() {
        NoteEditorFragment fragment = new NoteEditorFragment();
        // Should not throw NullPointerException with null fields
        fragment.saveScrollPosition();
        fragment.removeScrollListener();
    }

    @Test
    @MediumTest
    public void testSaveScrollPositionOnPauseWithAttachedFragment() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Simplenote app = (Simplenote) context;
        Bucket<Note> bucket = app.getNotesBucket();
        Note note = bucket.newObject("test_note_attached");
        note.setContent("Test note content for scroll persistence test\nLine 2\nLine 3\nLine 4");
        try {
            note.save();
        } catch (Exception ignored) {}

        Intent intent = new Intent(context, NoteEditorActivity.class);
        intent.putExtra(NoteEditorFragment.ARG_ITEM_ID, "test_note_attached");
        NoteEditorActivity activity = mActivityRule.launchActivity(intent);

        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            if (f instanceof NoteEditorFragment) {
                mAttachedFragment = (NoteEditorFragment) f;
                break;
            }
        }
        assertNotNull("NoteEditorFragment must be attached in NoteEditorActivity", mAttachedFragment);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SimplenoteEditText editText = activity.findViewById(R.id.note_content);
            if (editText != null) {
                editText.scrollTo(0, 180);
            }
        });

        // Trigger onPause by calling saveScrollPosition directly or pausing
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mAttachedFragment.saveScrollPosition();
        });

        SharedPreferences prefs = activity.getSharedPreferences(Simplenote.SCROLL_POSITION_PREFERENCES, Context.MODE_PRIVATE);
        assertEquals(180, prefs.getInt("test_note_attached", -1));
    }

    @Test
    @MediumTest
    public void testUpdateNoteSavesScrollBeforeReload() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Simplenote app = (Simplenote) context;
        Bucket<Note> bucket = app.getNotesBucket();
        Note oldNote = bucket.newObject("test_note_old");
        oldNote.setContent("Old note content");
        try {
            oldNote.save();
        } catch (Exception ignored) {}

        Note updatedNote = bucket.newObject("test_note_new");
        updatedNote.setContent("Updated note content from sync");
        try {
            updatedNote.save();
        } catch (Exception ignored) {}

        Intent intent = new Intent(context, NoteEditorActivity.class);
        intent.putExtra(NoteEditorFragment.ARG_ITEM_ID, "test_note_old");
        NoteEditorActivity activity = mActivityRule.launchActivity(intent);

        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            if (f instanceof NoteEditorFragment) {
                mAttachedFragment = (NoteEditorFragment) f;
                break;
            }
        }
        assertNotNull(mAttachedFragment);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SimplenoteEditText editText = activity.findViewById(R.id.note_content);
            if (editText != null) {
                editText.scrollTo(0, 320);
            }
            mAttachedFragment.updateNote(updatedNote);
        });

        SharedPreferences prefs = activity.getSharedPreferences(Simplenote.SCROLL_POSITION_PREFERENCES, Context.MODE_PRIVATE);
        assertEquals("Old note scroll position must be saved before switching to updated note",
                320, prefs.getInt("test_note_old", -1));
    }
}
