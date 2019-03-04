package com.automattic.simplenote;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.automattic.simplenote.utils.ThemeUtils;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.ArrayList;
import java.util.List;

public class NoteMarkdownFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    private Note mNote;
    private String mCss;
    private WebView mMarkdown;
    private boolean mIsLoadingNote;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Load note if we were passed an ID.
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            String key = arguments.getString(ARG_ITEM_ID);
            new loadNoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
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
    public boolean onOptionsItemSelected(MenuItem item) {
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
        mMarkdown.loadDataWithBaseURL(null, getMarkdownFormattedContent(mCss, text), "text/html", "utf-8", null);
    }

    public static String getMarkdownFormattedContent(String cssContent, String sourceContent) {
        String header = "<html><head>" +
                "<link href=\"https://fonts.googleapis.com/css?family=Noto+Serif\" rel=\"stylesheet\">" +
                cssContent + "</head><body>";

        List<Extension> extensions = new ArrayList<>();
        extensions.add(TablesExtension.create());
        extensions.add(StrikethroughExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        Node document = parser.parse(sourceContent);
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build();
        String parsedMarkdown = renderer.render(document);

        return header + "<div class=\"note-detail-markdown\">" + parsedMarkdown +
                "</div></body></html>";
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
                if (getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                }
            }
        }
    }
}
