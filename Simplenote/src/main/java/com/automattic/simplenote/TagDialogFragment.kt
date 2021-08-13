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
import android.widget.Button
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

    private var mButtonNegative: Button? = null
    private var mButtonNeutral: Button? = null
    private var mButtonPositive: Button? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.close()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = EditTagBinding.inflate(LayoutInflater.from(context))
        return buildDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                mButtonPositive!!.isEnabled = false
            } else {
                binding.inputTagName.error = null
                mButtonPositive!!.isEnabled = true
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
        mButtonNegative = dialogEditTag.getButton(DialogInterface.BUTTON_NEGATIVE)
        mButtonNeutral = dialogEditTag.getButton(DialogInterface.BUTTON_NEUTRAL)
        mButtonPositive = dialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE)

        // Set observers when views are available
        setObservers()
        viewModel.start(tag)
        showDialogRenameTag()
        binding.inputTagName.editText?.setText(tag.name)
    }

    private fun showDialogErrorConflict(canonical: String, tagOld: String) {
        dialogEditTag.setTitle(R.string.dialog_tag_conflict_title)
        binding.message.text = getString(R.string.dialog_tag_conflict_message, canonical, tagOld, canonical)
        mButtonNeutral!!.setText(R.string.back)
        mButtonPositive!!.setText(R.string.dialog_tag_conflict_button_positive)
        binding.message.visibility = View.VISIBLE
        binding.inputTagName.visibility = View.GONE
        mButtonNegative!!.visibility = View.GONE
        mButtonNeutral!!.visibility = View.VISIBLE
        mButtonNeutral!!.setOnClickListener { showDialogRenameTag() }
        mButtonPositive!!.setOnClickListener { viewModel.renameTag() }
    }

    private fun showDialogRenameTag() {
        dialogEditTag.setTitle(R.string.rename_tag)
        mButtonNegative!!.setText(R.string.cancel)
        mButtonPositive!!.setText(R.string.save)
        binding.message.visibility = View.GONE
        binding.inputTagName.visibility = View.VISIBLE
        mButtonNegative!!.visibility = View.VISIBLE
        mButtonNeutral!!.visibility = View.GONE
        mButtonNegative!!.setOnClickListener { viewModel.close() }
        mButtonPositive!!.setOnClickListener { viewModel.renameTagIfValid() }
    }

    companion object {
        @JvmField
        var DIALOG_TAG = "dialog_tag"
    }
}