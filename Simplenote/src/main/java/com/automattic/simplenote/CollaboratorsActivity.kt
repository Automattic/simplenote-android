package com.automattic.simplenote

import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.simplenote.databinding.ActivityCollaboratorsBinding
import com.automattic.simplenote.utils.CollaboratorsAdapter
import com.automattic.simplenote.utils.SimplenoteProgressDialogFragment
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.Event
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.UiState.*
import com.simperium.android.ProgressDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollaboratorsActivity : ThemedAppCompatActivity() {
    private val viewModel: CollaboratorsViewModel by viewModels()

    private var _progressDialog: SimplenoteProgressDialogFragment? = null
    private val progressDialog: SimplenoteProgressDialogFragment
        get() = _progressDialog!!

    companion object {
        const val NOTE_ID_ARG = "note_id"
        const val DIALOG_TAG = "dialog_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityCollaboratorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val noteId = intent.getStringExtra(NOTE_ID_ARG)
        if (noteId == null) {
            finish()
            return
        }

        binding.setupViews()
        binding.setObservers()

        viewModel.loadCollaborators(noteId)
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
        collaboratorsList.layoutManager = LinearLayoutManager(this@CollaboratorsActivity)

        buttonAddCollaborator.setOnClickListener { viewModel.clickAddCollaborator() }
    }

    private fun ActivityCollaboratorsBinding.setObservers() {
        viewModel.uiState.observe(this@CollaboratorsActivity, { uiState ->
            when (uiState) {
                Loading -> showProgressDialog()
                EmptyCollaborators -> handleEmptyCollaborators()
                is CollaboratorsList -> handleCollaboratorsList(uiState)
                NoteDeleted -> TODO()
                NoteInTrash -> TODO()
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
        emptyMessage.visibility = View.VISIBLE

        hideProgressDialog()

        val adapter = collaboratorsList.adapter as CollaboratorsAdapter
        adapter.submitList(uiState.collaborators)
    }

    private fun ActivityCollaboratorsBinding.handleEmptyCollaborators() {
        hideProgressDialog()
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

    private fun showProgressDialog() {
        _progressDialog = SimplenoteProgressDialogFragment.newInstance(getString(R.string.loading_collaborators))
        progressDialog.show(supportFragmentManager, ProgressDialogFragment.TAG)
    }

    private fun hideProgressDialog() {
        if (_progressDialog != null && !progressDialog.isHidden) {
            progressDialog.dismiss()
            _progressDialog = null
        }
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
}