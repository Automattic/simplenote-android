package com.automattic.simplenote

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.automattic.simplenote.databinding.AddCollaboratorBinding
import com.automattic.simplenote.viewmodels.AddCollaboratorViewModel


class AddCollaboratorFragment(private val noteId: String) : AppCompatDialogFragment(), DialogInterface.OnShowListener {
    private val viewModel: AddCollaboratorViewModel by viewModels()

    private var _dialogEditTag: AlertDialog? = null
    private val dialogEditTag get() = _dialogEditTag!!

    private var _binding: AddCollaboratorBinding? = null
    private val binding  get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = AddCollaboratorBinding.inflate(LayoutInflater.from(context))
        return buildDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _dialogEditTag = null
    }

    private fun buildDialog(): Dialog {
        val context: Context = ContextThemeWrapper(requireContext(), R.style.Dialog)
        _dialogEditTag = AlertDialog.Builder(context)
            .setView(binding.root)
            .setTitle(R.string.add_collaborator)
            .setNegativeButton(R.string.cancel) { _, _ -> viewModel.close() }
            .setPositiveButton(R.string.accept) { _, _ ->
                val collaborator = binding.collaboratorText.text.toString()
                viewModel.addCollaborator(noteId, collaborator)
            }
            .create()

        dialogEditTag.setOnShowListener(this)
        binding.collaboratorInput.editText?.doAfterTextChanged {
            if (binding.collaboratorInput.error != null) {
                binding.collaboratorInput.error = null
            }
        }

        return dialogEditTag
    }

    override fun onShow(dialog: DialogInterface?) {
        setObservers()
    }

    private fun setObservers() {
        viewModel.event.observe(this, { event ->
            when (event) {
                AddCollaboratorViewModel.Event.Close,
                AddCollaboratorViewModel.Event.CollaboratorAdded -> dismiss()
                AddCollaboratorViewModel.Event.InvalidCollaborator -> setErrorInputField()
                AddCollaboratorViewModel.Event.NoteDeleted -> TODO()
                AddCollaboratorViewModel.Event.NoteInTrash -> TODO()
            }
        })
    }

    private fun setErrorInputField() {
        binding.collaboratorInput.error = getString(R.string.invalid_collaborator)
    }
}