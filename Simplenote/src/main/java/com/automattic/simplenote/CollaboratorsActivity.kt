package com.automattic.simplenote

import android.os.Bundle
import android.text.SpannableString
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.simplenote.databinding.ActivityCollaboratorsBinding
import com.automattic.simplenote.utils.CollaboratorsAdapter
import com.automattic.simplenote.utils.SimplenoteProgressDialogFragment
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel
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
                CollaboratorsViewModel.Event.AddCollaboratorEvent -> TODO()
                is CollaboratorsViewModel.Event.RemoveCollaboratorEvent -> TODO()
                CollaboratorsViewModel.Event.CloseCollaboratorsEvent -> finish()
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
}