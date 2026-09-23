package com.automattic.simplenote

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.simplenote.databinding.ActivityCollaboratorsBinding
import com.automattic.simplenote.utils.CollaboratorsAdapter
import com.automattic.simplenote.utils.CollaboratorsAdapter.CollaboratorDataItem.*
import com.automattic.simplenote.utils.DisplayUtils
import com.automattic.simplenote.utils.DrawableUtils
import com.automattic.simplenote.utils.HtmlCompat
import com.automattic.simplenote.utils.IntentUtils
import com.automattic.simplenote.utils.SystemBarUtils
import com.automattic.simplenote.utils.getColorStr
import com.automattic.simplenote.utils.toast
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.Event
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.UiState.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollaboratorsActivity : ThemedAppCompatActivity() {
    private val viewModel: CollaboratorsViewModel by viewModels()

    companion object {
        const val NOTE_ID_ARG = "note_id"
        const val DIALOG_TAG = "dialog_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val noteId = intent.getStringExtra(NOTE_ID_ARG)
        if (noteId == null) {
            finish()
            return
        }

        with(ActivityCollaboratorsBinding.inflate(layoutInflater)) {
            setContentView(root)

            setupViews()
            setObservers()

            viewModel.loadCollaborators(noteId)

            // Setup edge-to-edge display with proper WindowInsets handling
            // Use auto-theming to properly handle status bar appearance based on theme
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            SystemBarUtils.setupEdgeToEdgeWithAutoTheming(
                this@CollaboratorsActivity,
                findViewById(R.id.main_parent_view),
                toolbar,
                collaboratorsList
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.collaborators_list, menu)
        DrawableUtils.tintMenuWithAttribute(this, menu, R.attr.toolbarIconColor)
        val searchMenuItem = menu.findItem(R.id.menu_search)
        val searchView = searchMenuItem.actionView as SearchView
        val searchEditFrame = searchView.findViewById<LinearLayout>(R.id.search_edit_frame)
        (searchEditFrame.layoutParams as LinearLayout.LayoutParams).leftMargin = 0

        val hintHexColor = getColorStr(R.color.text_title_disabled)
        searchView.queryHint = HtmlCompat.fromHtml(String.format(
            "<font color=\"%s\">%s</font>",
            hintHexColor,
            getString(R.string.search_collaborators_hint)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            viewModel.close()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        viewModel.uiState.observe(this@CollaboratorsActivity, { uiState ->
            menu?.findItem(R.id.menu_search)?.isVisible = when (uiState) {
                is EmptyCollaborators -> !uiState.allCollaboratorsRemoved && !uiState.searchUpdate
                else -> true
            }
        })

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()

        viewModel.startListeningChanges()
    }

    override fun onPause() {
        super.onPause()

        viewModel.stopListeningChanges()
    }

    private fun ActivityCollaboratorsBinding.setupViews() {
        setupToolbar()

        collaboratorsList.adapter = CollaboratorsAdapter(viewModel::clickRemoveCollaborator, viewModel::longClickRemoveCollaborator)
        collaboratorsList.isNestedScrollingEnabled = false
        collaboratorsList.layoutManager = LinearLayoutManager(this@CollaboratorsActivity)
        collaboratorsList.setEmptyView(empty.root)

        buttonAddCollaborator.setOnClickListener { viewModel.clickAddCollaborator() }
        buttonAddCollaborator.setOnApplyWindowInsetsListener { view, insets ->
            DisplayUtils.applyWindowInsetsForFloatingActionButton(insets, resources, view)
        }
        buttonAddCollaborator.setOnLongClickListener {
            viewModel.longClickAddCollaborator()
            true
        }

        empty.image.setImageResource(R.drawable.ic_collaborate_24dp)
        empty.title.text = getString(R.string.no_collaborators)
        empty.message.text = getString(R.string.add_email_collaborator_message)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = SpannableString(getString(R.string.collaborators))
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun ActivityCollaboratorsBinding.setObservers() {
        viewModel.uiState.observe(this@CollaboratorsActivity, { uiState ->
            when (uiState) {
                is EmptyCollaborators -> {
                    handleEmptyCollaborators(uiState.allCollaboratorsRemoved, uiState.searchUpdate)
                }
                is CollaboratorsList -> {
                    handleCollaboratorsList(uiState.collaborators, uiState.searchUpdate, uiState.searchQuery)
                }
                is NoteDeleted -> {
                    toast(R.string.collaborators_note_deleted, Toast.LENGTH_LONG)
                    navigateToNotesList()
                }
                is NoteInTrash -> {
                    toast(R.string.collaborators_note_trashed, Toast.LENGTH_LONG)
                    navigateToNotesList()
                }
            }
        })

        viewModel.event.observe(this@CollaboratorsActivity, { event ->
            when (event) {
                is Event.AddCollaboratorEvent -> showAddCollaboratorFragment(event)
                is Event.LongAddCollaboratorEvent -> showLongAddToast()
                is Event.LongRemoveCollaboratorEvent -> showLongRemoveToast()
                is Event.RemoveCollaboratorEvent -> showRemoveCollaboratorDialog(event)
                Event.CloseCollaboratorsEvent -> finish()
            }
        })
    }

    private fun ActivityCollaboratorsBinding.showLongAddToast() {
        if (buttonAddCollaborator.isHapticFeedbackEnabled) {
            buttonAddCollaborator.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        toast(R.string.add_collaborator)
    }

    private fun ActivityCollaboratorsBinding.showLongRemoveToast() {
        if (buttonAddCollaborator.isHapticFeedbackEnabled) {
            buttonAddCollaborator.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        toast(R.string.remove_collaborator)
    }

    private fun ActivityCollaboratorsBinding.handleCollaboratorsList(collaborators: List<String>, searchUpdate: Boolean, searchQuery: String?) {
        hideEmptyView()
        val items = listOf(HeaderItem) + collaborators.map { CollaboratorItem(it) }
        (collaboratorsList.adapter as CollaboratorsAdapter).submitList(items) {
            if (searchUpdate) {
                collaboratorsList.scrollToPosition(0)

                if (searchQuery != null) {
                    setEmptyViewSearch()
                } else {
                    setEmptyViewDefault()
                }
            }
        }
    }

    private fun ActivityCollaboratorsBinding.handleEmptyCollaborators(allCollaboratorsRemoved: Boolean, searchUpdate: Boolean) {
        showEmptyView()
        (collaboratorsList.adapter as CollaboratorsAdapter).submitList(emptyList()) {
            if (allCollaboratorsRemoved) {
                invalidateOptionsMenu()
                setEmptyViewDefault()
            } else if (searchUpdate) {
                collaboratorsList.scrollToPosition(0)
                setEmptyViewSearch()
            }
        }
    }

    private fun ActivityCollaboratorsBinding.hideEmptyView() {
        empty.image.visibility = View.GONE
        empty.title.visibility = View.GONE
        empty.message.visibility = View.GONE
    }

    private fun ActivityCollaboratorsBinding.showEmptyView() {
        empty.image.visibility = View.VISIBLE
        empty.title.visibility = View.VISIBLE
        empty.message.visibility = View.VISIBLE
    }

    private fun ActivityCollaboratorsBinding.setEmptyListImage(@DrawableRes image: Int) {
        if (image != -1) {
            empty.image.visibility = View.VISIBLE
            empty.image.setImageResource(image)
        } else {
            empty.image.visibility = View.GONE
        }
    }

    private fun ActivityCollaboratorsBinding.setEmptyListMessage(message: String?) {
        message?.let {
            empty.message.text = it
        }
    }

    private fun ActivityCollaboratorsBinding.setEmptyListTitle(title: String?) {
        title?.let {
            empty.title.text = it
        }
    }

    private fun ActivityCollaboratorsBinding.setEmptyViewDefault() {
        if (DisplayUtils.isLandscape(this@CollaboratorsActivity) &&
            !DisplayUtils.isLargeScreen(this@CollaboratorsActivity)
        ) {
            setEmptyListImage(-1)
        } else {
            setEmptyListImage(R.drawable.ic_collaborate_24dp)
        }

        setEmptyListMessage(getString(R.string.add_email_collaborator_message))
        setEmptyListTitle(getString(R.string.no_collaborators))
    }

    private fun ActivityCollaboratorsBinding.setEmptyViewSearch() {
        if (DisplayUtils.isLandscape(this@CollaboratorsActivity) &&
            !DisplayUtils.isLargeScreen(this@CollaboratorsActivity)
        ) {
            setEmptyListImage(-1)
        } else {
            setEmptyListImage(R.drawable.ic_search_24dp)
        }

        setEmptyListMessage("")
        setEmptyListTitle(getString(R.string.no_collaborators_search))
    }

    private fun showAddCollaboratorFragment(event: Event.AddCollaboratorEvent) {
        val dialog = AddCollaboratorFragment(event.noteId)
        dialog.show(supportFragmentManager.beginTransaction(), DIALOG_TAG)
    }

    private fun showRemoveCollaboratorDialog(event: Event.RemoveCollaboratorEvent) {
        val alert = AlertDialog.Builder(ContextThemeWrapper(this, R.style.Dialog))
        alert.setTitle(R.string.remove_collaborator)
        alert.setMessage(R.string.remove_collaborator_message)
        alert.setNegativeButton(R.string.cancel, null)
        alert.setPositiveButton(R.string.remove) {  _, _ -> viewModel.removeCollaborator(event.collaborator) }
        alert.show()
    }

    private fun navigateToNotesList() {
        val intent = IntentUtils.maybeAliasedIntent(applicationContext)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}
