package com.automattic.simplenote

import android.app.ActivityOptions
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.utils.*
import com.automattic.simplenote.viewmodels.TagsEvent
import com.automattic.simplenote.viewmodels.TagsEvent.*
import com.automattic.simplenote.viewmodels.TagsViewModel
import com.automattic.simplenote.widgets.EmptyViewRecyclerView
import com.automattic.simplenote.widgets.MorphSetup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagsActivity : ThemedAppCompatActivity() {
    private val viewModel: TagsViewModel by viewModels()

    private var mTagsList: EmptyViewRecyclerView? = null
    private var mButtonAdd: ImageButton? = null
    private var mEmptyViewImage: ImageView? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mEmptyViewText: TextView? = null
    private var tagItemAdapter: TagItemAdapter? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)
        tagItemAdapter = TagItemAdapter(
            viewModel::clickEditTag,
            viewModel::clickDeleteTag,
            viewModel::longClickDeleteTag
        )
        setupViews()
        setObservers()
        viewModel.start()
    }

    private fun setupViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val title = SpannableString(getString(R.string.edit_tags))
        if (supportActionBar != null) {
            supportActionBar!!.title = title
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        mTagsList = findViewById(R.id.list)
        mTagsList!!.adapter = tagItemAdapter
        mTagsList!!.layoutManager = LinearLayoutManager(this@TagsActivity)
        val emptyView = findViewById<View>(R.id.empty)
        mEmptyViewImage = emptyView.findViewById(R.id.image)
        mEmptyViewText = emptyView.findViewById(R.id.text)
        setLabelEmptyTagList()
        mTagsList!!.setEmptyView(emptyView)
        mButtonAdd = findViewById(R.id.button_add)
        mButtonAdd!!.setOnClickListener { viewModel.clickAddTag() }
        mButtonAdd!!.setOnLongClickListener {
            viewModel.longClickAddTag()
            true
        }
    }

    private fun setObservers() {
        viewModel.uiState.observe(this, { (tagItems, searchUpdate, searchQuery) ->
            tagItemAdapter!!.submitList(
                tagItems) {
                if (searchUpdate) {
                    mTagsList!!.scrollToPosition(0)
                    val isSearching = searchQuery != null
                    if (isSearching) {
                        setLabelEmptyTagListSearchResults()
                    } else {
                        setLabelEmptyTagList()
                    }
                }
            }
        }
        )

        viewModel.event.observe(this, { event: TagsEvent ->
            when (event) {
                is AddTagEvent -> startAddTagActivity()
                is LongAddTagEvent -> showLongAddToast()
                is FinishEvent -> finish()
                is EditTagEvent -> showTagDialogFragment(event)
                is DeleteTagEvent -> showDeleteDialog(event)
                is LongDeleteTagEvent -> showLongDeleteToast(event)
            }
        })
    }

    private fun showLongAddToast() {
        if (mButtonAdd!!.isHapticFeedbackEnabled) {
            mButtonAdd!!.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        toast(R.string.add_tag)
    }

    private fun showLongDeleteToast(event: LongDeleteTagEvent) {
        val v = event.view
        if (v.isHapticFeedbackEnabled) {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        toast(R.string.delete_tag)
    }

    private fun showDeleteDialog(event: DeleteTagEvent) {
        val alert = AlertDialog.Builder(ContextThemeWrapper(this@TagsActivity, R.style.Dialog))
        alert.setTitle(R.string.delete_tag)
        alert.setMessage(getString(R.string.confirm_delete_tag))
        alert.setNegativeButton(R.string.no, null)
        alert.setPositiveButton(
            R.string.yes
        ) { _: DialogInterface?, _: Int -> viewModel.deleteTag(event.tagItem) }
        alert.show()
    }

    private fun showTagDialogFragment(event: EditTagEvent) {
        val dialog = TagDialogFragment(event.tagItem.tag)
        dialog.show(supportFragmentManager.beginTransaction(), TagDialogFragment.DIALOG_TAG)
    }

    private fun startAddTagActivity() {
        val intent = Intent(this@TagsActivity, AddTagActivity::class.java)
        intent.putExtra(MorphSetup.EXTRA_SHARED_ELEMENT_COLOR_END,
            ThemeUtils.getColorFromAttribute(this@TagsActivity, R.attr.drawerBackgroundColor))
        intent.putExtra(MorphSetup.EXTRA_SHARED_ELEMENT_COLOR_START,
            ThemeUtils.getColorFromAttribute(this@TagsActivity, R.attr.fabColor))
        val options = ActivityOptions.makeSceneTransitionAnimation(this@TagsActivity, mButtonAdd, "shared_button")
        startActivityForResult(intent, REQUEST_ADD_TAG, options.toBundle())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.tags_list, menu)
        DrawableUtils.tintMenuWithAttribute(this@TagsActivity, menu, R.attr.toolbarIconColor)
        mSearchMenuItem = menu.findItem(R.id.menu_search)
        val searchView = mSearchMenuItem!!.actionView as SearchView
        val searchEditFrame = searchView.findViewById<LinearLayout>(R.id.search_edit_frame)
        (searchEditFrame.layoutParams as LinearLayout.LayoutParams).leftMargin = 0

        // Workaround for setting the search placeholder text color.
        val hintHexColor = getColorStr(R.color.text_title_disabled)
        searchView.queryHint = HtmlCompat.fromHtml(String.format(
            "<font color=\"%s\">%s</font>",
            hintHexColor,
            getString(R.string.search_tags_hint)
        ))
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(query: String): Boolean {
                    if (mSearchMenuItem!!.isActionViewExpanded) {
                        viewModel.search(query)
                    }
                    return true
                }

                override fun onQueryTextSubmit(queryText: String): Boolean {
                    return true
                }
            }
        )
        searchView.setOnCloseListener {
            viewModel.closeSearch()
            false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        DisplayUtils.disableScreenshotsIfLocked(this)
        viewModel.startListeningTagChanges()
    }

    public override fun onPause() {
        super.onPause()
        viewModel.stopListeningTagChanges()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_ADD_TAG) {
            viewModel.updateOnResult()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setLabelEmptyTagList() {
        setEmptyListImage(R.drawable.ic_tag_24dp)
        setEmptyListMessage(getString(R.string.empty_tags))
    }

    private fun setLabelEmptyTagListSearchResults() {
        if (DisplayUtils.isLandscape(this@TagsActivity) &&
            !DisplayUtils.isLargeScreen(this@TagsActivity)
        ) {
            setEmptyListImage(-1)
        } else {
            setEmptyListImage(R.drawable.ic_search_24dp)
        }
        setEmptyListMessage(getString(R.string.empty_tags_search))
    }

    private fun setEmptyListImage(@DrawableRes image: Int) {
        if (mEmptyViewImage != null) {
            if (image != -1) {
                mEmptyViewImage!!.visibility = View.VISIBLE
                mEmptyViewImage!!.setImageResource(image)
            } else {
                mEmptyViewImage!!.visibility = View.GONE
            }
        }
    }

    private fun setEmptyListMessage(message: String?) {
        if (mEmptyViewText != null && message != null) {
            mEmptyViewText!!.text = message
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            viewModel.close()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val REQUEST_ADD_TAG = 9000
    }
}