package com.automattic.simplenote.authentication.magiclink

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.automattic.simplenote.R
import com.automattic.simplenote.authentication.SignInFragment
import com.automattic.simplenote.utils.HtmlCompat
import com.automattic.simplenote.utils.NetworkUtils
import com.automattic.simplenote.viewmodels.CompleteMagicLinkViewModel
import com.automattic.simplenote.viewmodels.MagicLinkUiState
import com.simperium.android.ProgressDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MagicLinkConfirmationFragment : Fragment() {

    private var progressDialogFragment: ProgressDialogFragment? = null

    private var codeEditText: EditText? = null
    private var actionCodeButton: Button? = null
    private var loginWithPassword: TextView? = null

    private val completeMagicLinkViewModel: CompleteMagicLinkViewModel by viewModels()

    companion object {
        const val PARAM_USERNAME = "param_username"
        fun newInstance(username: String) : Fragment {
            val magicLinkFragment = MagicLinkConfirmationFragment()
            val bundle = Bundle()
            bundle.putString(PARAM_USERNAME, username)
            magicLinkFragment.arguments = bundle
            return magicLinkFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_magic_link_code, container, false)
        initUi(view)
        completeMagicLinkViewModel.magicLinkUiState.observe(this.viewLifecycleOwner) { state ->
            when (state) {
                is MagicLinkUiState.Loading -> {
                    showProgressDialog(getString(state.messageRes))
                }
                is MagicLinkUiState.Success -> {
                    completeMagicLinkViewModel.resetState()
                    hideDialogProgress()
                    activity?.finish()
                }
                is MagicLinkUiState.Error -> {
                    completeMagicLinkViewModel.resetState()
                    hideDialogProgress()
                    Toast.makeText(context, getString(state.messageRes), Toast.LENGTH_LONG).show()
                }
            }
        }
        return view
    }

    private fun initUi(view: View) {
        val textView: TextView = view.findViewById(R.id.magic_link_enter_code_message)
        arguments?.getString(PARAM_USERNAME)?.let {
            val bolded = "<br/><b>$it</b>"
            textView.text = HtmlCompat.fromHtml(
                String.format(
                    getString(R.string.magic_link_confirm_code_message),
                    bolded
                )
            )
        }
        codeEditText = view.findViewById(R.id.confirmation_code_textfield)
        actionCodeButton = view.findViewById(R.id.confirmation_code_button)

        actionCodeButton?.isEnabled = false
        actionCodeButton?.setOnClickListener {
            val code = codeEditText?.text?.toString()
            val username = arguments?.getString(PARAM_USERNAME)
            if (!code.isNullOrBlank() && !username.isNullOrBlank()) {
                if (NetworkUtils.isNetworkAvailable(view.context)) {
                    completeMagicLinkViewModel.completeLogin(username, code, true)
                } else {
                    Toast.makeText(view.context, getString(R.string.simperium_dialog_message_network), Toast.LENGTH_SHORT).show()
                }
            }
        }
        codeEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no-ops
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // no-ops
            }

            override fun afterTextChanged(s: Editable?) {
                actionCodeButton?.isEnabled = s != null && s.length >= 6
            }
        })
        loginWithPassword = view.findViewById(R.id.login_with_password_button)
        loginWithPassword?.setOnClickListener { _ ->
            val username: String? = arguments?.getString(PARAM_USERNAME)
            SignInFragment.showLoginWithPassword(activity, username)
        }
    }

    private fun showProgressDialog(label: String) {
        progressDialogFragment =
            ProgressDialogFragment.newInstance(label)
        progressDialogFragment?.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Simperium)
        progressDialogFragment?.show(requireFragmentManager(), ProgressDialogFragment.TAG)
    }

    private fun hideDialogProgress() {
        progressDialogFragment?.let {
            if (!it.isHidden) {
                it.dismiss()
                progressDialogFragment = null
            }
        }
    }
}
