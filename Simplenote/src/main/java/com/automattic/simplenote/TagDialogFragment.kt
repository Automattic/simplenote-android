package com.automattic.simplenote

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.res.Configuration
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.automattic.simplenote.databinding.EditTagBinding
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.DialogUtils
import com.automattic.simplenote.viewmodels.TagDialogEvent
import com.automattic.simplenote.viewmodels.TagDialogEvent.*
import com.automattic.simplenote.viewmodels.TagDialogViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagDialogFragment(private val tag: Tag) : AppCompatDialogFragment(), OnShowListener {
    private val viewModel: TagDialogViewModel by viewModels()

    private var _dialogEditTag: AlertDialog? = null
    private val dialogEditTag get() = _dialogEditTag!!

    private var _binding: EditTagBinding? = null
    private val binding  get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = EditTagBinding.inflate(LayoutInflater.from(context))
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
            .setTitle(R.string.rename_tag)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialogEditTag.setOnShowListener(this)
        binding.inputTagName.editText?.doAfterTextChanged { s -> viewModel.updateUiState(s.toString()) }

        return dialogEditTag
    }

    private fun setObservers() {
        viewModel.uiState.observe(this, { (_, _, errorMsg) ->
            if (errorMsg != null) {
                binding.inputTagName.error = getString(errorMsg)
                dialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
            } else {
                binding.inputTagName.error = null
                dialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
            }
        })

        viewModel.event.observe(this, { event: TagDialogEvent ->
            when (event) {
                CloseEvent, FinishEvent -> dismiss()
                ShowErrorEvent -> {
                    val context = requireContext()
                    DialogUtils.showDialogWithEmail(context, context.getString(R.string.rename_tag_message))
                }
                is ConflictEvent -> {
                    val (canonicalTagName, oldTagName) = event
                    showDialogErrorConflict(canonicalTagName, oldTagName)
                }
            }
        })
    }

    override fun onShow(dialog: DialogInterface) {
        setObservers()
        startUiState()
        showDialogRenameTag()
    }

    private fun startUiState() {
        viewModel.start(tag)
        binding.inputTagName.editText?.setText(tag.name)
    }

    /**
     * Change the dialog layout to show a conflict message in which the user can merge two tags or cancel the merge.
     */
    private fun showDialogErrorConflict(canonical: String, tagOld: String) {
        binding.message.text = getString(R.string.dialog_tag_conflict_message, canonical, tagOld, canonical)
        binding.message.visibility = View.VISIBLE
        binding.inputTagName.visibility = View.GONE

        dialogEditTag.setTitle(R.string.dialog_tag_conflict_title)

        val positiveButton = dialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.text = getString(R.string.dialog_tag_conflict_button_positive)
        positiveButton.visibility = View.VISIBLE
        positiveButton.setOnClickListener { viewModel.renameTag() }

        val negativeButton = dialogEditTag.getButton(DialogInterface.BUTTON_NEGATIVE)
        negativeButton.visibility = View.GONE

        val neutralButton = dialogEditTag.getButton(DialogInterface.BUTTON_NEUTRAL)
        neutralButton.text = getString(R.string.back)
        neutralButton.visibility = View.VISIBLE
        neutralButton.setOnClickListener { showDialogRenameTag() }
    }

    /**
     * Change the dialog layout to show the default view in which the user can update the tag name.
     */
    private fun showDialogRenameTag() {
        binding.message.visibility = View.GONE
        binding.inputTagName.visibility = View.VISIBLE

        dialogEditTag.setTitle(R.string.rename_tag)

        val positiveButton = dialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.text = getString(R.string.save)
        positiveButton.visibility = View.VISIBLE
        positiveButton.setOnClickListener { viewModel.renameTagIfValid() }

        val negativeButton = dialogEditTag.getButton(DialogInterface.BUTTON_NEGATIVE)
        negativeButton.text = getString(R.string.cancel)
        negativeButton.visibility = View.VISIBLE
        negativeButton.setOnClickListener { viewModel.close() }

        val neutralButton = dialogEditTag.getButton(DialogInterface.BUTTON_NEUTRAL)
        neutralButton.visibility = View.GONE
    }

    companion object {
        @JvmField
        var DIALOG_TAG = "dialog_tag"
    }
}
