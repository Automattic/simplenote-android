package com.automattic.simplenote;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.ContextUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.NoteUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.commonsware.cwac.anddown.AndDown;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import java.lang.ref.SoftReference;

public class NoteMarkdownFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    private Note mNote;
    private String mCss;
    private WebView mMarkdown;
    private boolean mIsLoadingNote;

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.note_markdown, menu);

        DrawableUtils.tintMenuWithAttribute(
            requireContext(),
            menu,
            R.attr.toolbarIconColor
        );

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            DrawableUtils.setMenuItemAlpha(item, 0.3);  // 0.3 is 30% opacity.
        }

        if (mNote != null) {
            MenuItem viewPublishedNoteItem = menu.findItem(R.id.menu_view_info);
            viewPublishedNoteItem.setVisible(true);

            MenuItem trashItem = menu.findItem(R.id.menu_delete).setTitle(R.string.undelete);

            if (mNote.isDeleted()) {
                trashItem.setTitle(R.string.undelete);
                trashItem.setIcon(R.drawable.ic_trash_restore_24dp);
            } else {
                trashItem.setTitle(R.string.delete);
                trashItem.setIcon(R.drawable.ic_trash_24dp);
            }

            DrawableUtils.tintMenuItemWithAttribute(getActivity(), trashItem, R.attr.toolbarIconColor);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Load note if we were passed an ID.
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            String key = arguments.getString(ARG_ITEM_ID);
            new loadNoteTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
        }
        setHasOptionsMenu(true);
        mCss = ThemeUtils.isLightTheme(requireContext())
                ? ContextUtils.readCssFile(requireContext(), "light.css")
                : ContextUtils.readCssFile(requireContext(), "dark.css");
        View layout = inflater.inflate(R.layout.fragment_note_markdown, container, false);
        mMarkdown = layout.findViewById(R.id.markdown);
        return layout;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                if (!isAdded()) return false;
                deleteNote();
                return true;
            case android.R.id.home:
                if (!isAdded()) return false;
                requireActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteNote() {
        NoteUtils.deleteNote(mNote, getActivity());
        requireActivity().finish();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        // Disable share and delete actions until note is loaded.
        if (mIsLoadingNote) {
            menu.findItem(R.id.menu_delete).setEnabled(false);
        } else {
            menu.findItem(R.id.menu_delete).setEnabled(true);
        }

        super.onPrepareOptionsMenu(menu);
    }

    public void updateMarkdown(String text) {
        mMarkdown.loadDataWithBaseURL(null, getMarkdownFormattedContent(mCss, text), "text/html", "utf-8", null);
    }

    public static String getMarkdownFormattedContent(String cssContent, String sourceContent) {
        String header = "<html><head>" +
                "<link href=\"https://fonts.googleapis.com/css?family=Noto+Serif\" rel=\"stylesheet\">" +
                cssContent + "</head><body>";

        String parsedMarkdown = new AndDown().markdownToHtml(
                sourceContent,
                AndDown.HOEDOWN_EXT_STRIKETHROUGH | AndDown.HOEDOWN_EXT_FENCED_CODE |
                        AndDown.HOEDOWN_EXT_QUOTE | AndDown.HOEDOWN_EXT_TABLES,
                AndDown.HOEDOWN_HTML_ESCAPE
        );

        return header + "<div class=\"note-detail-markdown\">" + parsedMarkdown +
                "</div></body></html>";
    }

    private static class loadNoteTask extends AsyncTask<String, Void, Void> {

        private SoftReference<NoteMarkdownFragment> fragmentRef;

        private loadNoteTask(NoteMarkdownFragment context) {
            fragmentRef = new SoftReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            NoteMarkdownFragment fragment = fragmentRef.get();
            fragment.mIsLoadingNote = true;
        }

        @Override
        protected Void doInBackground(String... args) {
            NoteMarkdownFragment fragment = fragmentRef.get();
            FragmentActivity activity = fragment.getActivity();

            if (activity == null) {
                return null;
            }

            String noteID = args[0];
            Simplenote application = (Simplenote) activity.getApplication();
            Bucket<Note> notesBucket = application.getNotesBucket();

            try {
                fragment.mNote = notesBucket.get(noteID);
            } catch (BucketObjectMissingException exception) {
                // TODO: Handle a missing note
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            NoteMarkdownFragment fragment = fragmentRef.get();

            fragment.mIsLoadingNote = false;

            if (fragment.mNote != null) {
                if (fragment.getActivity() != null) {
                    fragment.getActivity().invalidateOptionsMenu();
                }
            }
        }
    }
}
