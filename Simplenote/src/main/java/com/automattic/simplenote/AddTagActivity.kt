package com.automattic.simplenote

import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.automattic.simplenote.databinding.ActivityTagAddBinding
import com.automattic.simplenote.utils.DisplayUtils
import com.automattic.simplenote.utils.HtmlCompat
import com.automattic.simplenote.utils.ThemeUtils
import com.automattic.simplenote.viewmodels.AddTagViewModel
import com.automattic.simplenote.widgets.MorphCircleToRectangle
import com.automattic.simplenote.widgets.MorphSetup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddTagActivity : AppCompatActivity() {
    private val viewModel: AddTagViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.setTheme(this)
        super.onCreate(savedInstanceState)
        val binding: ActivityTagAddBinding = ActivityTagAddBinding.inflate(layoutInflater)

        binding.setObservers()
        binding.setupLayout()
        binding.setupViews()

        viewModel.start()

        setContentView(binding.root)
    }

    private fun ActivityTagAddBinding.setupViews() {
        title.text = getString(R.string.add_tag)
        tagInput.doAfterTextChanged { s -> viewModel.updateUiState(s.toString()) }
        buttonNegative.setOnClickListener { viewModel.close() }
        buttonPositive.setOnClickListener { viewModel.saveTag() }
    }

    private fun ActivityTagAddBinding.setupLayout() {
        val widthScreen = resources.displayMetrics.widthPixels
        val widthMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56f, resources.displayMetrics).toInt()
        val widthMaximum =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 312f, resources.displayMetrics).toInt()
        layout.layoutParams.width = widthMaximum.coerceAtMost(widthScreen - widthMargin)
        layout.requestLayout()
        (layout.parent as View).setOnClickListener { viewModel.close() }
        layout.setOnClickListener(null)
        MorphSetup.setSharedElementTransitions(
            this@AddTagActivity,
            layout,
            resources.getDimensionPixelSize(R.dimen.corner_radius_dialog)
        )
    }

    private fun ActivityTagAddBinding.setObservers() {
        // Observe changes in the UI state
        viewModel.uiState.observe(this@AddTagActivity, { (_, errorMsg) ->
            // Validate if the current state has an error
            if (errorMsg != null) {
                tagLayout.error = getString(errorMsg)
                buttonPositive.isEnabled = false
            } else {
                // If there is not an error, enable save button
                tagLayout.error = null
                buttonPositive.isEnabled = true
            }
        })

        viewModel.event.observe(this@AddTagActivity, { event: AddTagViewModel.Event? ->
            when (event) {
                AddTagViewModel.Event.START -> {
                    buttonPositive.isEnabled = false
                    Handler().postDelayed(
                        {
                            tagInput.requestFocus()
                            DisplayUtils.showKeyboard(tagInput)
                        },
                        MorphCircleToRectangle.DURATION.toLong()
                    )
                }
                AddTagViewModel.Event.CLOSE -> finishAfterTransition()
                AddTagViewModel.Event.FINISH -> {
                    DisplayUtils.hideKeyboard(tagInput)
                    setResult(RESULT_OK)
                    finishAfterTransition()
                }
                AddTagViewModel.Event.SHOW_ERROR -> {
                    DisplayUtils.hideKeyboard(tagInput)
                    showDialogError()
                }
            }
        })
    }

    private fun showDialogError() {
        val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.Dialog))
            .setTitle(R.string.error)
            .setMessage(
                HtmlCompat.fromHtml(
                    String.format(
                        getString(R.string.dialog_tag_error_message),
                        getString(R.string.dialog_tag_error_message_email),
                        "<span style=\"color:#",
                        Integer.toHexString(
                            ThemeUtils.getColorFromAttribute(
                                this,
                                R.attr.colorAccent
                            ) and 0xffffff
                        ),
                        "\">",
                        "</span>"
                    )
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()

        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }
}
