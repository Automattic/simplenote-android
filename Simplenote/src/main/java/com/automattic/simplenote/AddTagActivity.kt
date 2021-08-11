package com.automattic.simplenote

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.LinearLayout
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
import java.util.*

@AndroidEntryPoint
class AddTagActivity : AppCompatActivity() {
    private val viewModel: AddTagViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.setTheme(this)
        super.onCreate(savedInstanceState)
        val binding: ActivityTagAddBinding = ActivityTagAddBinding.inflate(layoutInflater)

        setObservers(binding)
        setupLayout(binding)
        setupViews(binding)

        viewModel.start()

        setContentView(binding.root)
    }

    private fun setupViews(binding: ActivityTagAddBinding) {
        binding.title.text = getString(R.string.add_tag)
        binding.tagInput.doAfterTextChanged { s -> viewModel.updateUiState(s.toString()) }
        binding.buttonNegative.setOnClickListener { viewModel.close() }
        binding.buttonPositive.setOnClickListener { viewModel.saveTag() }
    }

    private fun setupLayout(binding: ActivityTagAddBinding) {
        val widthScreen = resources.displayMetrics.widthPixels
        val widthMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56f, resources.displayMetrics).toInt()
        val widthMaximum =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 312f, resources.displayMetrics).toInt()
        val layout: LinearLayout = binding.layout
        layout.layoutParams.width = widthMaximum.coerceAtMost(widthScreen - widthMargin)
        layout.requestLayout()
        (layout.parent as View).setOnClickListener { viewModel.close() }
        layout.setOnClickListener(null)
        MorphSetup.setSharedElementTransitions(
            this,
            layout,
            resources.getDimensionPixelSize(R.dimen.corner_radius_dialog)
        )
    }

    private fun setObservers(binding: ActivityTagAddBinding) {
        // Observe changes in the UI state
        viewModel.uiState.observe(this, { (_, errorMsg) ->
            // Validate if the current state has an error
            if (errorMsg != null) {
                binding.tagLayout.error = getString(errorMsg)
                binding.buttonPositive.isEnabled = false
            } else {
                // If there is not an error, enable save button
                binding.tagLayout.error = null
                binding.buttonPositive.isEnabled = true
            }
        })

        viewModel.event.observe(this, { event: AddTagViewModel.Event? ->
            when (event) {
                AddTagViewModel.Event.START -> {
                    binding.buttonPositive.isEnabled = false
                    Handler().postDelayed(
                        {
                            binding.tagInput.requestFocus()
                            DisplayUtils.showKeyboard(binding.tagInput)
                        },
                        MorphCircleToRectangle.DURATION.toLong()
                    )
                }
                AddTagViewModel.Event.CLOSE -> finishAfterTransition()
                AddTagViewModel.Event.FINISH -> {
                    DisplayUtils.hideKeyboard(binding.tagInput)
                    setResult(RESULT_OK)
                    finishAfterTransition()
                }
                AddTagViewModel.Event.SHOW_ERROR -> {
                    DisplayUtils.hideKeyboard(binding.tagInput)
                    showDialogError()
                }
            }
        })
    }

    private fun showDialogError() {
        val dialog = AlertDialog.Builder(ContextThemeWrapper(this@AddTagActivity, R.style.Dialog))
            .setTitle(R.string.error)
            .setMessage(
                HtmlCompat.fromHtml(
                    String.format(
                        getString(R.string.dialog_tag_error_message),
                        getString(R.string.dialog_tag_error_message_email),
                        "<span style=\"color:#",
                        Integer.toHexString(
                            ThemeUtils.getColorFromAttribute(
                                this@AddTagActivity,
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
        (Objects.requireNonNull<Any?>(dialog.findViewById(android.R.id.message)) as TextView).movementMethod =
            LinkMovementMethod.getInstance()
    }
}
