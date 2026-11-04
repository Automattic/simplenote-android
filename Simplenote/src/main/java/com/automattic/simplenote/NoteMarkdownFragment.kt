package com.automattic.simplenote

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.automattic.simplenote.Simplenote.SCROLL_POSITION_PREFERENCES
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_NOTE
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.utils.AppLog
import com.automattic.simplenote.utils.AppLog.Type
import com.automattic.simplenote.utils.BrowserUtils
import com.automattic.simplenote.utils.ContextUtils
import com.automattic.simplenote.utils.DrawableUtils
import com.automattic.simplenote.utils.NetworkUtils
import com.automattic.simplenote.utils.NoteUtils
import com.automattic.simplenote.utils.SimplenoteLinkify
import com.automattic.simplenote.utils.SimplenoteLinkify.SIMPLENOTE_LINK_PREFIX
import com.automattic.simplenote.utils.ThemeUtils
import com.automattic.simplenote.utils.markdown.SimplenoteMarkdownFlavorDescriptor
import com.google.android.material.snackbar.Snackbar
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.lang.ref.SoftReference

class NoteMarkdownFragment : Fragment(), Bucket.Listener<Note> {

    companion object {
        const val ARG_ITEM_ID = "item_id"

        @JvmStatic
        fun getMarkdownFormattedContent(cssContent: String, sourceContent: String): String {
            val header = "<html><head>" +
                    "<link href=\"https://fonts.googleapis.com/css?family=Noto+Serif\" rel=\"stylesheet\">" +
                    "<meta name=\"viewport\" content=\"width=device-width,minimum-scale=1,initial-scale=1\">\n" +
                    cssContent + "</head><body>"

            val flavour = SimplenoteMarkdownFlavorDescriptor()
            val normalizedContent = sourceContent.replace("\r\n", "\n").replace('\r', '\n')
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(normalizedContent)
            var parsedMarkdown = HtmlGenerator(normalizedContent, parsedTree, flavour).generateHtml()

            // Set auto alignment for lists, tables, and quotes based on language of start.
            parsedMarkdown = parsedMarkdown
                .replace("<ol>", "<ol dir=\"auto\">")
                .replace("<ul>", "<ul dir=\"auto\">")
                .replace("<table>", "<table dir=\"auto\">")
                .replace("<blockquote>", "<blockquote dir=\"auto\">")

            return header + "<div class=\"note-detail-markdown\">" + parsedMarkdown +
                    "</div></body></html>"
        }
    }

    private var mNotesBucket: Bucket<Note>? = null
    private var mNote: Note? = null
    private var mPreferences: SharedPreferences? = null
    private var mCss: String? = null
    private var mMarkdown: WebView? = null
    private var mIsLoadingNote = false

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.note_markdown, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)

        DrawableUtils.tintMenuWithAttribute(
            requireContext(),
            menu,
            R.attr.toolbarIconColor
        )

        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            DrawableUtils.setMenuItemAlpha(item, 0.3) // 0.3 is 30% opacity.
        }

        mNote?.let { note ->
            val viewPublishedNoteItem = menu.findItem(R.id.menu_info)
            viewPublishedNoteItem.isVisible = true
            val trashItem = menu.findItem(R.id.menu_trash)

            if (note.isDeleted) {
                trashItem.setTitle(R.string.restore)
            } else {
                trashItem.setTitle(R.string.trash)
            }

            DrawableUtils.tintMenuItemWithAttribute(activity, trashItem, R.attr.toolbarIconColor)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        AppLog.add(Type.SCREEN, "Created (NoteMarkdownFragment)")
        mNotesBucket = (requireActivity().application as Simplenote).notesBucket
        mPreferences = requireContext().getSharedPreferences(SCROLL_POSITION_PREFERENCES, Context.MODE_PRIVATE)

        // Load note if we were passed an ID.
        val arguments = arguments
        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            val key = arguments.getString(ARG_ITEM_ID)
            LoadNoteTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key)
        }

        setHasOptionsMenu(true)
        val layout: View

        if (BrowserUtils.isWebViewInstalled(requireContext())) {
            layout = inflater.inflate(R.layout.fragment_note_markdown, container, false)
            (layout as NestedScrollView).setOnScrollChangeListener { _, _, scrollY, _, _ ->
                mNote?.let { note ->
                    mPreferences?.edit()?.putInt(note.simperiumKey, scrollY)?.apply()
                }
            }
            mMarkdown = layout.findViewById(R.id.markdown)

            val delay = requireContext().resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            mMarkdown?.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)

                    Handler(Looper.getMainLooper()).postDelayed({
                        mNote?.let { note ->
                            note.simperiumKey?.let { key ->
                                (layout as NestedScrollView).smoothScrollTo(0, mPreferences?.getInt(key, 0) ?: 0)
                            }
                        }
                    }, delay)
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()

                    if (url.startsWith(SimplenoteLinkify.SIMPLENOTE_LINK_PREFIX)) {
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.INTERNOTE_LINK_TAPPED,
                            AnalyticsTracker.CATEGORY_LINK,
                            "internote_link_tapped_markdown"
                        )
                        SimplenoteLinkify.openNote(requireActivity(), url.replace(SIMPLENOTE_LINK_PREFIX, ""))
                    } else {
                        BrowserUtils.launchBrowserOrShowError(requireContext(), url)
                    }

                    return true
                }
            }
            mCss = ContextUtils.readCssFile(requireContext(), ThemeUtils.getCssFromStyle(requireContext()))
        } else {
            layout = inflater.inflate(R.layout.fragment_note_error, container, false)
            layout.findViewById<View>(R.id.error).visibility = View.VISIBLE
            layout.findViewById<View>(R.id.button).setOnClickListener {
                BrowserUtils.launchBrowserOrShowError(requireContext(), BrowserUtils.URL_WEB_VIEW)
            }
        }

        return layout
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (!isAdded) {
                    return false
                }
                requireActivity().finish()
                true
            }
            R.id.menu_delete -> {
                mNote?.let { note ->
                    NoteUtils.showDialogDeletePermanently(requireActivity(), note)
                }
                true
            }
            R.id.menu_collaborators -> {
                navigateToCollaborators()
                true
            }
            R.id.menu_trash -> {
                if (!isAdded) {
                    return false
                }
                deleteNote()
                true
            }
            R.id.menu_copy_internal -> {
                AnalyticsTracker.track(
                    AnalyticsTracker.Stat.INTERNOTE_LINK_COPIED,
                    AnalyticsTracker.CATEGORY_LINK,
                    "internote_link_copied_markdown"
                )

                if (!isAdded) {
                    return false
                }

                mNote?.let { note ->
                    if (BrowserUtils.copyToClipboard(requireContext(), SimplenoteLinkify.getNoteLinkWithTitle(note.title, note.simperiumKey))) {
                        mMarkdown?.let { markdown ->
                            Snackbar.make(markdown, R.string.link_copied, Snackbar.LENGTH_SHORT).show()
                        }
                    } else {
                        mMarkdown?.let { markdown ->
                            Snackbar.make(markdown, R.string.link_copied_failure, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToCollaborators() {
        if (activity == null || mNote == null) {
            return
        }

        val intent = Intent(requireActivity(), CollaboratorsActivity::class.java)
        intent.putExtra(CollaboratorsActivity.NOTE_ID_ARG, mNote?.simperiumKey)
        startActivity(intent)

        AnalyticsTracker.track(
            AnalyticsTracker.Stat.EDITOR_COLLABORATORS_ACCESSED,
            CATEGORY_NOTE,
            "collaborators_ui_accessed"
        )
    }

    private fun deleteNote() {
        NoteUtils.deleteNote(mNote, activity)
        requireActivity().finish()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Show delete action only when note is in Trash.
        menu.findItem(R.id.menu_delete).isVisible = mNote != null && mNote!!.isDeleted
        // Disable trash action until note is loaded.
        menu.findItem(R.id.menu_trash).isEnabled = !mIsLoadingNote

        val pinItem = menu.findItem(R.id.menu_pin)
        val publishItem = menu.findItem(R.id.menu_publish)
        val copyLinkItem = menu.findItem(R.id.menu_copy)
        val markdownItem = menu.findItem(R.id.menu_markdown)
        val copyLinkInternalItem = menu.findItem(R.id.menu_copy_internal)

        mNote?.let { note ->
            pinItem.isChecked = note.isPinned
            publishItem.isChecked = note.isPublished
            markdownItem.isChecked = note.isMarkdownEnabled
        }

        pinItem.isEnabled = false
        publishItem.isEnabled = false
        copyLinkItem.isEnabled = false
        markdownItem.isEnabled = false
        copyLinkInternalItem.isEnabled = true

        super.onPrepareOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        mNotesBucket?.removeListener(this)
        AppLog.add(Type.SYNC, "Removed note bucket listener (NoteMarkdownFragment)")
        AppLog.add(Type.SCREEN, "Destroyed (NoteMarkdownFragment)")
    }

    override fun onResume() {
        super.onResume()
        // First inflation of the webview may invalidate the value of uiMode,
        // so we re-apply it to make sure that the webview has the right css files
        // Check https://issuetracker.google.com/issues/37124582 for more details
        (requireActivity() as AppCompatActivity).delegate.applyDayNight()
        checkWebView()
        mNotesBucket?.addListener(this)
        AppLog.add(Type.SYNC, "Added note bucket listener (NoteMarkdownFragment)")
        AppLog.add(Type.NETWORK, NetworkUtils.getNetworkInfo(requireContext()))
        AppLog.add(Type.SCREEN, "Resumed (NoteMarkdownFragment)")
    }

    override fun onBeforeUpdateObject(bucket: Bucket<Note>, note: Note) {
    }

    override fun onDeleteObject(bucket: Bucket<Note>, note: Note) {
    }

    override fun onNetworkChange(bucket: Bucket<Note>, type: Bucket.ChangeType, key: String) {
    }

    override fun onSaveObject(bucket: Bucket<Note>, note: Note) {
        if (note.equals(mNote)) {
            mNote = note
            requireActivity().invalidateOptionsMenu()
        }

        AppLog.add(
            Type.SYNC,
            "Saved note callback in NoteMarkdownFragment (ID: " + note.simperiumKey +
                    " / Title: " + note.title +
                    " / Characters: " + NoteUtils.getCharactersCount(note.content) +
                    " / Words: " + NoteUtils.getWordCount(note.content) + ")"
        )
    }

    private fun checkWebView() {
        // When a WebView is installed and mMarkdown is null, a WebView was not installed when the
        // fragment was created.  So, open the note again to show the markdown preview.
        if (BrowserUtils.isWebViewInstalled(requireContext()) && mMarkdown == null) {
            mNote?.let { note ->
                SimplenoteLinkify.openNote(requireActivity(), note.simperiumKey)
            }
        }
    }

    fun updateMarkdown(text: String) {
        mMarkdown?.let { markdown ->
            mCss?.let { css ->
                markdown.loadDataWithBaseURL(null, getMarkdownFormattedContent(css, text), "text/html", "utf-8", null)
            }
        }
    }

    override fun onLocalQueueChange(bucket: Bucket<Note>, queuedObjects: Set<String>) {
    }

    override fun onSyncObject(bucket: Bucket<Note>, key: String) {
    }

    private class LoadNoteTask(context: NoteMarkdownFragment) : AsyncTask<String, Void, Void>() {
        private val mNoteMarkdownFragmentReference: SoftReference<NoteMarkdownFragment> = SoftReference(context)

        override fun onPreExecute() {
            val fragment = mNoteMarkdownFragmentReference.get()
            fragment?.mIsLoadingNote = true
        }

        override fun doInBackground(vararg args: String): Void? {
            val fragment = mNoteMarkdownFragmentReference.get() ?: return null
            val activity = fragment.activity ?: return null

            val noteID = args[0]
            val application = activity.application as Simplenote
            val notesBucket = application.notesBucket

            try {
                fragment.mNote = notesBucket.get(noteID)
            } catch (exception: BucketObjectMissingException) {
                // TODO: Handle a missing note
            }

            return null
        }

        override fun onPostExecute(nada: Void?) {
            val fragment = mNoteMarkdownFragmentReference.get()
            fragment?.mIsLoadingNote = false
            fragment?.requireActivity()?.invalidateOptionsMenu()
        }
    }
}
