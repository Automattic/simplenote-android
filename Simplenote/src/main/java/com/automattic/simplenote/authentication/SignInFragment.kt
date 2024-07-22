package com.automattic.simplenote.authentication

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.automattic.simplenote.R
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.analytics.AnalyticsTracker.Stat
import com.automattic.simplenote.authentication.passkey.PasskeyManager
import com.automattic.simplenote.utils.NetworkUtils
import com.automattic.simplenote.viewmodels.MagicLinkRequestUiState
import com.automattic.simplenote.viewmodels.PasskeyUiState
import com.automattic.simplenote.viewmodels.PasskeyViewModel
import com.automattic.simplenote.viewmodels.RequestMagicLinkViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment: MagicLinkableFragment() {

    val viewModel: RequestMagicLinkViewModel by viewModels()

    private val passkeyViewModel: PasskeyViewModel by viewModels()

    private var loginWithPassword: TextView? = null

    private var loginWithPasskey: Button? = null

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        loginWithPassword = view.findViewById(R.id.login_with_password_button)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Passkeys are only supported in API 28 and higher (https://developer.android.com/identity/sign-in/credential-manager)
            loginWithPasskey = view.findViewById(R.id.passkey_action_button)
            loginWithPasskey?.visibility = View.VISIBLE
            loginWithPasskey?.isEnabled = false
            loginWithPassword?.setOnClickListener { _ ->
                activity?.let { act ->
                    val intent = Intent(act, SimplenoteCredentialsActivity::class.java)
                    intent.putExtra("EXTRA_IS_LOGIN", true)
                    val currentEmail = getEmailEditText()?.text.toString()
                    if (!TextUtils.isEmpty(currentEmail)) {
                        intent.putExtra(Intent.EXTRA_EMAIL, currentEmail)
                    }
                    this.startActivity(intent)
                    act.finish()
                }
            }
            loginWithPasskey?.setOnClickListener { _ ->
                val email = emailField?.editableText.toString()
                if (email.isNotBlank() && isValidEmail(email)) {
                    passkeyViewModel.prepareAuthChallenge(email)
                }
            }
        }
        return view
    }

    override fun emailEntered(s: Editable, isValid: Boolean) {
        super.emailEntered(s, isValid)
        loginWithPasskey?.let {
            it.isEnabled = isValid
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel.magicLinkRequestUiState.observe(this.viewLifecycleOwner) { state ->
            when (state) {
                is MagicLinkRequestUiState.Loading -> {
                    showProgressDialog(getString(state.messageRes))
                }
                is MagicLinkRequestUiState.Error -> hideDialogProgress()
                is MagicLinkRequestUiState.Success -> {
                    hideDialogProgress()
                    showConfirmationScreen(state.username, false)
                    AnalyticsTracker.track(
                        Stat.USER_REQUESTED_LOGIN_LINK,
                        AnalyticsTracker.CATEGORY_USER,
                        "user_requested_login_link"
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            passkeyViewModel.passkeyUiState.observe(this.viewLifecycleOwner) { state ->
                when (state) {
                    is PasskeyUiState.PasskeyLoading -> {
                        showProgressDialog(getString(state.msg))
                    }

                    is PasskeyUiState.PasskeyPrepareAuthChallengeRequest -> {
                        // Prepare Auth Challenge for login
                        hideDialogProgress()
                        passkeyViewModel.resetState()
                        PasskeyManager.getCredential(
                            requireContext(),
                            emailField?.editableText.toString(),
                            state.challengeJson,
                            passkeyViewModel
                        )
                    }

                    is PasskeyUiState.PasskeyVerifyAuthChallengeRequest -> {
                        // Use this token to sign in with Simplenote
                        hideDialogProgress()
                        passkeyViewModel.resetState()
                        val simplenote = requireActivity().application as Simplenote
                        simplenote.loginWithToken(state.username, state.token)

                        activity?.finish()
                    }

                    is PasskeyUiState.PasskeyError -> {
                        hideDialogProgress()
                        Toast.makeText(requireContext(), getString(state.message), Toast.LENGTH_LONG).show()
                    }

                    else -> {
                        // no-ops
                    }
                }
            }
            // TODO: Autofill is experimental and not finalized yet.
//            passkeyViewModel.attemptAutofill()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun actionButtonText(): String = getString(R.string.magic_link_login)

    override fun onActionButtonClicked(view: View, emailEditText: EditText) {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            viewModel?.requestLogin(emailEditText.text.toString())
        } else {
            showDialogError(getString(R.string.simperium_dialog_message_network))
        }
    }

}
