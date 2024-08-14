package com.automattic.simplenote.authentication

import android.app.Activity
import android.content.Intent

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.Editable

import android.net.Uri
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import androidx.fragment.app.viewModels
import com.automattic.simplenote.R
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.analytics.AnalyticsTracker.Stat
import com.automattic.simplenote.authentication.passkey.PasskeyManager
import com.automattic.simplenote.utils.NetworkUtils
import com.automattic.simplenote.utils.StrUtils
import com.automattic.simplenote.utils.WordPressUtils
import com.automattic.simplenote.viewmodels.MagicLinkRequestUiState
import com.automattic.simplenote.viewmodels.PasskeyUiState
import com.automattic.simplenote.viewmodels.PasskeyViewModel
import com.automattic.simplenote.viewmodels.RequestMagicLinkViewModel
import dagger.hilt.android.AndroidEntryPoint
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import java.util.UUID

const val TAG = "SignInFragment"

@AndroidEntryPoint
class SignInFragment: MagicLinkableFragment() {

    val viewModel: RequestMagicLinkViewModel by viewModels()

    private val passkeyViewModel: PasskeyViewModel by viewModels()

    private var loginWithPassword: TextView? = null

    private var authState: String? = null

    private var authService: AuthorizationService? = null

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        val data = result.data
        if (data == null) {
            Log.d(TAG, "Result code does not match WordPressUtils.OAUTH_ACTIVITY_CODE, or data was null")
            return@registerForActivityResult
        }

        val authorizationResponse = AuthorizationResponse.fromIntent(data)
        val authorizationException = AuthorizationException.fromIntent(data)

        if (authorizationException != null) {
            val dataUri: Uri = data.data ?: return@registerForActivityResult
            if (StrUtils.isSameStr(dataUri.getQueryParameter("code"), "1")) {
                showDialogError(getString(R.string.wpcom_log_in_error_unverified))
            } else {
                showDialogError(getString(R.string.wpcom_log_in_error_generic))
            }
        } else if (authorizationResponse != null) {
            // Save token and finish activity.
            val authSuccess = WordPressUtils.processAuthResponse(
                activity?.application as Simplenote?,
                authorizationResponse,
                authState,
                true
            )
            if (!authSuccess) {
                showDialogError(getString(R.string.wpcom_log_in_error_generic))
            } else {
                AnalyticsTracker.track(
                    Stat.WPCC_LOGIN_SUCCEEDED,
                    AnalyticsTracker.CATEGORY_USER,
                    "wpcc_login_succeeded_signin_activity"
                )
                activity?.finish()
            }
        }
    }

    private var loginWithPasskey: Button? = null

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        loginWithPassword = view.findViewById(R.id.login_with_password_button)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Passkeys are only supported in API 28 and higher (https://developer.android.com/identity/sign-in/credential-manager)
            loginWithPasskey = view.findViewById(R.id.passkey_action_button)
            loginWithPasskey?.visibility = View.VISIBLE
            loginWithPasskey?.isEnabled = false
            loginWithPasskey?.setOnClickListener { _ ->
                val email = emailField?.editableText.toString()
                if (email.isNotBlank() && isValidEmail(email)) {
                    passkeyViewModel.prepareAuthChallenge(email)
                }
            }
        }
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

        val loginWithWordpress: Button? = view.findViewById(R.id.button_login_with_wordpress)
        loginWithWordpress?.setOnClickListener {
            val authRequestBuilder = WordPressUtils.getWordPressAuthorizationRequestBuilder()

            // Set unique state value.
            authState = "app-" + UUID.randomUUID()
            authRequestBuilder.setState(authState)

            val request = authRequestBuilder.build()
            authService = AuthorizationService(loginWithWordpress.context)
            authService?.getAuthorizationRequestIntent(request)?.let {
                resultLauncher.launch(it)
            }

            AnalyticsTracker.track(
                Stat.WPCC_BUTTON_PRESSED,
                AnalyticsTracker.CATEGORY_USER,
                "wpcc_button_press_signin_activity"
            )
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
                is MagicLinkRequestUiState.Error -> {
                    hideDialogProgress()
                    if (state.code == 429) {
                        val email = getEmailEditText()
                        showLoginWithPassword(activity, email?.text?.toString())
                    }
                    Toast.makeText(context, getString(state.messageRes), Toast.LENGTH_LONG).show()
                }
                is MagicLinkRequestUiState.Success -> {
                    viewModel.resetState()
                    hideDialogProgress()
                    showConfirmationScreen(state.username, false)
                    AnalyticsTracker.track(
                        Stat.USER_REQUESTED_LOGIN_LINK,
                        AnalyticsTracker.CATEGORY_USER,
                        "user_requested_login_link"
                    )
                }

                else -> {
                    // no-ops
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
            viewModel.requestLogin(emailEditText.text.toString())
        } else {
            showDialogError(getString(R.string.simperium_dialog_message_network))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resultLauncher.unregister()
        authService?.dispose()
    }

    companion object {
        fun showLoginWithPassword(activity: Activity?, username: String?) {
            activity?.let { act ->
                val intent = Intent(act, SimplenoteCredentialsActivity::class.java)
                intent.putExtra("EXTRA_IS_LOGIN", true)
                if (!username.isNullOrBlank()) {
                    intent.putExtra(Intent.EXTRA_EMAIL, username)
                }
                activity.startActivity(intent)
                act.finish()
            }
        }
    }

}
