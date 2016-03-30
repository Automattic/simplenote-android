package com.automattic.simplenote;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.PrefUtils;
import com.commonsware.cwac.anddown.AndDown;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import java.util.Calendar;

public class NoteMarkdownFragment extends Fragment {
    private Note mNote;
    private String mCss;
    private WebView mMarkdown;
    private boolean mIsLoadingNote;

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static final String ARG_ITEM_ID = "item_id";

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_markdown, menu);

        if (mNote != null) {
            MenuItem viewPublishedNoteItem = menu.findItem(R.id.menu_view_info);
            viewPublishedNoteItem.setVisible(true);

            MenuItem trashItem = menu.findItem(R.id.menu_delete).setTitle(R.string.undelete);

            if (mNote.isDeleted()) {
                trashItem.setTitle(R.string.undelete);
            } else {
                trashItem.setTitle(R.string.delete);
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Load note if we were passed an ID.
        Bundle arguments = getArguments();

        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            String key = arguments.getString(ARG_ITEM_ID);
            new loadNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
        }

        setHasOptionsMenu(true);

        View layout = inflater.inflate(R.layout.fragment_note_markdown, container, false);
        mMarkdown = (WebView) layout.findViewById(R.id.markdown);

        switch (PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_THEME, THEME_LIGHT)) {
            case THEME_DARK:
                mCss = "<link rel=\"stylesheet\" type=\"text/css\" href=\"dark.css\" />";
                break;
            case THEME_LIGHT:
                mCss = "<link rel=\"stylesheet\" type=\"text/css\" href=\"light.css\" />";
                break;
        }

        return layout;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                if (mNote != null) {
                    Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mNote.getContent());
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_note)));
                    AnalyticsTracker.track(
                            AnalyticsTracker.Stat.EDITOR_NOTE_CONTENT_SHARED,
                            AnalyticsTracker.CATEGORY_NOTE,
                            "action_bar_share_button"
                    );
                }
                return true;
            case R.id.menu_delete:
                if (!isAdded()) {
                    return false;
                }

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
                if (!isAdded()) {
                    return false;
                }

                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Disable share and delete actions until note is loaded.
        if (mIsLoadingNote) {
            menu.findItem(R.id.menu_share).setEnabled(false);
            menu.findItem(R.id.menu_delete).setEnabled(false);
        } else {
            menu.findItem(R.id.menu_share).setEnabled(true);
            menu.findItem(R.id.menu_delete).setEnabled(true);
        }

        super.onPrepareOptionsMenu(menu);
    }

    public void updateMarkdown(String text) {
        mMarkdown.loadDataWithBaseURL("file:///android_asset/", mCss +
                new AndDown().markdownToHtml(text), "text/html", "utf-8", null);
    }

    private class loadNoteTask extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
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
            } catch (BucketObjectMissingException exception) {
                // TODO: Handle a missing note
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            mIsLoadingNote = false;

            if (mNote != null) {
                getActivity().invalidateOptionsMenu();
            }
        }
    }
}
