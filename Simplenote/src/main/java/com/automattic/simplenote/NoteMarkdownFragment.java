package com.automattic.simplenote;

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

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.ContextUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.NoteUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.commonsware.cwac.anddown.AndDown;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

public class NoteMarkdownFragment extends Fragment {
    private Note mNote;
    private String mCss;
    private String mRawCss;
    private WebView mMarkdown;
    private boolean mIsLoadingNote;

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static final String ARG_ITEM_ID = "item_id";

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_markdown, menu);

        DrawableUtils.tintMenuWithResource(getActivity(), menu, R.color.simplenote_blue_disabled);

        if (mNote != null) {
            MenuItem viewPublishedNoteItem = menu.findItem(R.id.menu_view_info);
            viewPublishedNoteItem.setVisible(true);

            MenuItem trashItem = menu.findItem(R.id.menu_delete).setTitle(R.string.undelete);
            DrawableUtils.tintMenuItemWithAttribute(getActivity(), trashItem, R.attr.actionBarTextColor);

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
                mRawCss = ContextUtils.readCssFile(getActivity(), "dark.css");
                break;
            case THEME_LIGHT:
                mRawCss = ContextUtils.readCssFile(getActivity(), "light.css");
                break;
        }

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCss();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Disable share and delete actions until note is loaded.
        if (mIsLoadingNote) {
            menu.findItem(R.id.menu_delete).setEnabled(false);
        } else {
            menu.findItem(R.id.menu_delete).setEnabled(true);
        }

        super.onPrepareOptionsMenu(menu);
    }

    public void updateMarkdown(String text) {
        mMarkdown.loadData(mCss + new AndDown().markdownToHtml(text), "text/html", "utf-8");
    }

    private void updateCss() {
        if (mRawCss == null) {
            mCss = "";
            return;
        }

        int fontSize = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_FONT_SIZE, 14);

        mCss = "<style>"
                + mRawCss.replace("${H1-SIZE}", String.valueOf(fontSize + 16))
                .replace("${H2-SIZE}", String.valueOf(fontSize + 8))
                .replace("${H3-SIZE}", String.valueOf(fontSize + 3))
                .replace("${P-SIZE}", String.valueOf(fontSize))
                .replace("${H5-SIZE}", String.valueOf(fontSize - 2))
                .replace("${H6-SIZE}", String.valueOf(fontSize - 5))
                + "</style>";
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
