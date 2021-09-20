package com.automattic.simplenote

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.simplenote.databinding.ActivityCollaboratorsBinding
import com.automattic.simplenote.utils.CollaboratorsAdapter
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
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            viewModel.close()
            return true
        }
        return super.onOptionsItemSelected(item)
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
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = SpannableString(getString(R.string.collaborators))
            setDisplayHomeAsUpEnabled(true)
        }

        // Hide all views while loading
        sharedMessage.visibility = View.GONE
        collaboratorsList.visibility = View.GONE
        dividerLine.visibility = View.GONE
        buttonAddCollaborator.visibility = View.GONE
        emptyMessage.visibility = View.GONE

        collaboratorsList.adapter = CollaboratorsAdapter(viewModel::clickRemoveCollaborator)
        collaboratorsList.isNestedScrollingEnabled = false
        collaboratorsList.layoutManager = LinearLayoutManager(this@CollaboratorsActivity)

        buttonAddCollaborator.setOnClickListener { viewModel.clickAddCollaborator() }
    }

    private fun ActivityCollaboratorsBinding.setObservers() {
        viewModel.uiState.observe(this@CollaboratorsActivity, { uiState ->
            when (uiState) {
                EmptyCollaborators -> handleEmptyCollaborators()
                is CollaboratorsList -> handleCollaboratorsList(uiState)
                NoteDeleted -> {
                    toast(R.string.collaborators_note_deleted, Toast.LENGTH_LONG)
                    navigateToNotesList()
                }
                NoteInTrash -> {
                    toast(R.string.collaborators_note_trashed, Toast.LENGTH_LONG)
                    navigateToNotesList()
                }
            }
        })

        viewModel.event.observe(this@CollaboratorsActivity, { event ->
            when (event) {
                is Event.AddCollaboratorEvent -> showAddCollaboratorFragment(event)
                is Event.RemoveCollaboratorEvent -> showRemoveCollaboratorDialog(event)
                Event.CloseCollaboratorsEvent -> finish()
            }
        })
    }

    private fun ActivityCollaboratorsBinding.handleCollaboratorsList(uiState: CollaboratorsList) {
        sharedMessage.visibility = View.VISIBLE
        collaboratorsList.visibility = View.VISIBLE
        dividerLine.visibility = View.VISIBLE
        buttonAddCollaborator.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE

        val adapter = collaboratorsList.adapter as CollaboratorsAdapter
        adapter.submitList(uiState.collaborators)
    }

    private fun ActivityCollaboratorsBinding.handleEmptyCollaborators() {
        // Hide all views while loading
        sharedMessage.visibility = View.GONE
        collaboratorsList.visibility = View.GONE
        dividerLine.visibility = View.GONE
        buttonAddCollaborator.visibility = View.VISIBLE
        emptyMessage.visibility = View.VISIBLE
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
        alert.setPositiveButton(R.string.remove) { _: DialogInterface?, _: Int ->
            viewModel.removeCollaborator(event.collaborator)
        }
        alert.show()
    }

    private fun navigateToNotesList() {
        val intent = Intent(applicationContext, NotesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}
