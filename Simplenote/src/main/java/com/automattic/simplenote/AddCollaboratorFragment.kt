package com.automattic.simplenote

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.automattic.simplenote.databinding.AddCollaboratorBinding
import com.automattic.simplenote.utils.DisplayUtils
import com.automattic.simplenote.viewmodels.AddCollaboratorViewModel
import com.automattic.simplenote.viewmodels.AddCollaboratorViewModel.Event
import com.automattic.simplenote.widgets.MorphCircleToRectangle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddCollaboratorFragment(private val noteId: String) : AppCompatDialogFragment(), DialogInterface.OnShowListener {
    private val viewModel: AddCollaboratorViewModel by viewModels()

    private var _dialogEditTag: AlertDialog? = null
    private val dialogEditTag get() = _dialogEditTag!!

    private var _binding: AddCollaboratorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return buildDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _dialogEditTag = null
    }

    private fun buildDialog(): Dialog {
        val context = ContextThemeWrapper(requireContext(), R.style.Dialog)
        _binding = AddCollaboratorBinding.inflate(LayoutInflater.from(context))
        _dialogEditTag = AlertDialog.Builder(context)
            .setView(binding.root)
            .setTitle(R.string.add_collaborator)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.accept, null)
            .create()

        dialogEditTag.setOnShowListener(this)
        binding.collaboratorInput.editText?.doAfterTextChanged {
            // Clean error message when the user types something new in the field
            if (binding.collaboratorInput.error != null) {
                binding.collaboratorInput.error = null
            }
        }

        return dialogEditTag
    }

    override fun onShow(dialog: DialogInterface?) {
        setupViews()
        setObservers()
    }

    private fun setupViews() {
        val positiveButton = dialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val collaborator = binding.collaboratorText.text.toString()
            viewModel.addCollaborator(noteId, collaborator)
        }

        // Request focus for the text input after screen renders
        Handler().postDelayed(
            {
                binding.collaboratorText.requestFocus()
                DisplayUtils.showKeyboard(binding.collaboratorText)
            },
            MorphCircleToRectangle.DURATION.toLong()
        )
    }

    private fun setObservers() {
        viewModel.event.observe(this, { event ->
            when (event) {
                Event.Close,
                Event.NoteDeleted, // In case the note is deleted, the activity handles it.
                Event.NoteInTrash, // In case the note is trashed, the activity handles it.
                Event.CollaboratorAdded -> dismiss()
                Event.InvalidCollaborator -> setErrorInputField(R.string.invalid_collaborator)
                Event.CollaboratorCurrentUser -> setErrorInputField(R.string.collaborator_is_current_user)
            }
        })
    }

    private fun setErrorInputField(@StringRes message: Int) {
        binding.collaboratorInput.error = getString(message)
    }
}
