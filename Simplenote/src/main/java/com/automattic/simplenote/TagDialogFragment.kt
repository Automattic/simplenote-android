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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.DialogUtils
import com.automattic.simplenote.viewmodels.TagDialogEvent
import com.automattic.simplenote.viewmodels.TagDialogEvent.*
import com.automattic.simplenote.viewmodels.TagDialogViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagDialogFragment(private val tag: Tag) : AppCompatDialogFragment(), OnShowListener {
    private val viewModel: TagDialogViewModel by viewModels()

    private var mDialogEditTag: AlertDialog? = null
    private var mButtonNegative: Button? = null
    private var mButtonNeutral: Button? = null
    private var mButtonPositive: Button? = null
    private var mEditTextTag: TextInputEditText? = null
    private var mEditTextLayout: TextInputLayout? = null
    private var mMessage: TextView? = null
    private var mClickListenerNegativeRename: View.OnClickListener? = null
    private var mClickListenerNeutralConflict: View.OnClickListener? = null
    private var mClickListenerPositiveConflict: View.OnClickListener? = null
    private var mClickListenerPositiveRename: View.OnClickListener? = null


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.close()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setupDialog()
        return mDialogEditTag!!
    }

    private fun setupDialog() {
        val context: Context = ContextThemeWrapper(requireContext(), R.style.Dialog)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.rename_tag)
        builder.setNegativeButton(R.string.cancel, null)
        builder.setPositiveButton(R.string.save, null)
        val view = LayoutInflater.from(context).inflate(R.layout.edit_tag, null)
        mEditTextLayout = view.findViewById(R.id.input_tag_name)
        mEditTextTag = mEditTextLayout!!.editText as TextInputEditText?
        mMessage = view.findViewById(R.id.message)
        if (mEditTextTag != null) {
            mEditTextTag!!.doAfterTextChanged { s -> viewModel.updateUiState(s.toString()) }
        }
        builder.setView(view)
        mDialogEditTag = builder.create()
        mDialogEditTag!!.setOnShowListener(this)
        mClickListenerNegativeRename = View.OnClickListener { viewModel.close() }
        mClickListenerPositiveRename = View.OnClickListener { viewModel.renameTagIfValid() }
        mClickListenerNeutralConflict = View.OnClickListener { showDialogRenameTag() }
        mClickListenerPositiveConflict = View.OnClickListener { viewModel.renameTag() }
    }

    private fun setObservers() {
        viewModel.uiState.observe(this, { (_, _, errorMsg) ->
            if (errorMsg != null) {
                mEditTextLayout!!.error = getString(errorMsg)
                mButtonPositive!!.isEnabled = false
            } else {
                mEditTextLayout!!.error = null
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
        mButtonNegative = mDialogEditTag!!.getButton(DialogInterface.BUTTON_NEGATIVE)
        mButtonNeutral = mDialogEditTag!!.getButton(DialogInterface.BUTTON_NEUTRAL)
        mButtonPositive = mDialogEditTag!!.getButton(DialogInterface.BUTTON_POSITIVE)

        // Set observers when views are available
        setObservers()
        viewModel.start(tag)
        showDialogRenameTag()
        mEditTextTag!!.setText(tag.name)
    }

    private fun showDialogErrorConflict(canonical: String, tagOld: String) {
        mDialogEditTag!!.setTitle(R.string.dialog_tag_conflict_title)
        mMessage!!.text = getString(R.string.dialog_tag_conflict_message, canonical, tagOld, canonical)
        mButtonNeutral!!.setText(R.string.back)
        mButtonPositive!!.setText(R.string.dialog_tag_conflict_button_positive)
        mMessage!!.visibility = View.VISIBLE
        mEditTextLayout!!.visibility = View.GONE
        mButtonNegative!!.visibility = View.GONE
        mButtonNeutral!!.visibility = View.VISIBLE
        mButtonNeutral!!.setOnClickListener(mClickListenerNeutralConflict)
        mButtonPositive!!.setOnClickListener(mClickListenerPositiveConflict)
    }

    private fun showDialogRenameTag() {
        mDialogEditTag!!.setTitle(R.string.rename_tag)
        mButtonNegative!!.setText(R.string.cancel)
        mButtonPositive!!.setText(R.string.save)
        mMessage!!.visibility = View.GONE
        mEditTextLayout!!.visibility = View.VISIBLE
        mButtonNegative!!.visibility = View.VISIBLE
        mButtonNeutral!!.visibility = View.GONE
        mButtonNegative!!.setOnClickListener(mClickListenerNegativeRename)
        mButtonPositive!!.setOnClickListener(mClickListenerPositiveRename)
    }

    companion object {
        @JvmField
        var DIALOG_TAG = "dialog_tag"
    }
}