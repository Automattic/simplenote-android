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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.simplenote.databinding.ActivityTagsBinding
import com.automattic.simplenote.utils.*
import com.automattic.simplenote.viewmodels.TagsEvent
import com.automattic.simplenote.viewmodels.TagsEvent.*
import com.automattic.simplenote.viewmodels.TagsViewModel
import com.automattic.simplenote.widgets.MorphSetup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagsActivity : ThemedAppCompatActivity() {
    private val viewModel: TagsViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityTagsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setupViews()
        binding.setObservers()

        viewModel.start()
    }

    private fun ActivityTagsBinding.setupViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = SpannableString(getString(R.string.edit_tags))
            setDisplayHomeAsUpEnabled(true)
        }

        list.adapter = TagItemAdapter(
            viewModel::clickEditTag,
            viewModel::clickDeleteTag,
            viewModel::longClickDeleteTag
        )
        list.layoutManager = LinearLayoutManager(this@TagsActivity)

        setLabelEmptyTagList()
        list.setEmptyView(empty.root)

        buttonAdd.setOnClickListener { viewModel.clickAddTag() }
        buttonAdd.setOnLongClickListener {
            viewModel.longClickAddTag()
            true
        }
    }

    private fun ActivityTagsBinding.setObservers() {
        viewModel.uiState.observe(this@TagsActivity, { (tagItems, searchUpdate, searchQuery) ->
            val adapter = list.adapter as TagItemAdapter
            adapter.submitList(tagItems) {
                if (searchUpdate) {
                    list.scrollToPosition(0)
                    if (searchQuery != null) {
                        setLabelEmptyTagListSearchResults()
                    } else {
                        setLabelEmptyTagList()
                    }
                }
            }
        }
        )

        viewModel.event.observe(this@TagsActivity, { event: TagsEvent ->
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

    private fun ActivityTagsBinding.showLongAddToast() {
        if (buttonAdd.isHapticFeedbackEnabled) {
            buttonAdd.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
        val alert = AlertDialog.Builder(ContextThemeWrapper(this, R.style.Dialog))
        alert.setTitle(R.string.delete_tag)
        alert.setMessage(getString(R.string.confirm_delete_tag))
        alert.setNegativeButton(R.string.no, null)
        alert.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int -> viewModel.deleteTag(event.tagItem) }
        alert.show()
    }

    private fun showTagDialogFragment(event: EditTagEvent) {
        val dialog = TagDialogFragment(event.tagItem.tag)
        dialog.show(supportFragmentManager.beginTransaction(), TagDialogFragment.DIALOG_TAG)
    }

    @Suppress("DEPRECATION")
    private fun ActivityTagsBinding.startAddTagActivity() {
        val intent = Intent(this@TagsActivity, AddTagActivity::class.java)
        intent.putExtra(MorphSetup.EXTRA_SHARED_ELEMENT_COLOR_END,
            ThemeUtils.getColorFromAttribute(this@TagsActivity, R.attr.drawerBackgroundColor))
        intent.putExtra(MorphSetup.EXTRA_SHARED_ELEMENT_COLOR_START,
            ThemeUtils.getColorFromAttribute(this@TagsActivity, R.attr.fabColor))
        val options = ActivityOptions.makeSceneTransitionAnimation(this@TagsActivity, buttonAdd, "shared_button")
        startActivityForResult(intent, REQUEST_ADD_TAG, options.toBundle())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.tags_list, menu)
        DrawableUtils.tintMenuWithAttribute(this, menu, R.attr.toolbarIconColor)
        val searchMenuItem = menu.findItem(R.id.menu_search)
        val searchView = searchMenuItem.actionView as SearchView
        val searchEditFrame = searchView.findViewById<LinearLayout>(R.id.search_edit_frame)
        (searchEditFrame.layoutParams as LinearLayout.LayoutParams).leftMargin = 0


        val hintHexColor = getColorStr(R.color.text_title_disabled)
        searchView.queryHint = HtmlCompat.fromHtml(String.format(
            "<font color=\"%s\">%s</font>",
            hintHexColor,
            getString(R.string.search_tags_hint)
        ))

        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(query: String): Boolean {
                    if (searchMenuItem.isActionViewExpanded) {
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

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_ADD_TAG) {
            viewModel.updateOnResult()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun ActivityTagsBinding.setLabelEmptyTagList() {
        setEmptyListImage(R.drawable.ic_tag_24dp)
        setEmptyListMessage(getString(R.string.empty_tags))
    }

    private fun ActivityTagsBinding.setLabelEmptyTagListSearchResults() {
        if (DisplayUtils.isLandscape(this@TagsActivity) &&
            !DisplayUtils.isLargeScreen(this@TagsActivity)
        ) {
            setEmptyListImage(-1)
        } else {
            setEmptyListImage(R.drawable.ic_search_24dp)
        }
        setEmptyListMessage(getString(R.string.empty_tags_search))
    }

    private fun ActivityTagsBinding.setEmptyListImage(@DrawableRes image: Int) {
        if (image != -1) {
            empty.image.visibility = View.VISIBLE
            empty.image.setImageResource(image)
        } else {
            empty.image.visibility = View.GONE
        }
    }

    private fun ActivityTagsBinding.setEmptyListMessage(message: String?) {
        message?.let {
            empty.text.text = it
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
