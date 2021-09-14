package com.automattic.simplenote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.automattic.simplenote.databinding.ActivityCollaboratorsBinding
import com.automattic.simplenote.utils.SimplenoteProgressDialogFragment
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel
import com.simperium.android.ProgressDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollaboratorsActivity : AppCompatActivity() {
    private val viewModel: CollaboratorsViewModel by viewModels()

    private var _progressDialog: SimplenoteProgressDialogFragment? = null
    private val progressDialog: SimplenoteProgressDialogFragment
        get() = _progressDialog!!

    companion object {
        const val NOTE_ID_ARG = "note_id"
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

        binding.setObservers()
        viewModel.loadCollaborators(noteId)

    }

    private fun ActivityCollaboratorsBinding.setObservers() {
        viewModel.uiState.observe(this@CollaboratorsActivity, { uiState ->
            when (uiState) {
                CollaboratorsViewModel.UiState.Loading -> showProgressDialog()
                CollaboratorsViewModel.UiState.EmptyCollaborators -> handleEmptyCollaborators()
                is CollaboratorsViewModel.UiState.CollaboratorsList -> TODO()
                CollaboratorsViewModel.UiState.NoteDeleted -> TODO()
                CollaboratorsViewModel.UiState.NoteInTrash -> TODO()
            }
        })
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

    private fun ActivityCollaboratorsBinding.showProgressDialog() {
        // Hide all views while loading
        sharedMessage.visibility = View.GONE
        collaboratorsList.visibility = View.GONE
        dividerLine.visibility = View.GONE
        buttonAddCollaborator.visibility = View.GONE
        emptyMessage.visibility = View.GONE

        _progressDialog = SimplenoteProgressDialogFragment.newInstance(getString(R.string.loading_collaborators))
        progressDialog.show(supportFragmentManager, ProgressDialogFragment.TAG)
    }

    private fun hideProgressDialog() {
        if (_progressDialog != null && !progressDialog.isHidden) {
            progressDialog.dismiss()
            _progressDialog = null
        }
    }
}