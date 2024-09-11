package com.automattic.simplenote.authentication

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
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
import com.automattic.simplenote.utils.NetworkUtils
import com.automattic.simplenote.utils.StrUtils
import com.automattic.simplenote.utils.WordPressUtils
import com.automattic.simplenote.viewmodels.MagicLinkRequestUiState
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

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

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
        val manualLoginTextView = view.findViewById<TextView>(R.id.sign_in_login_manually)
        val message = getString(R.string.signin_login_with_email_manually);
        val span = StrUtils.generateClickableSpannableString(LOGIN_MANUALLY_SUBSTRING, message
        ) {
            val email = getEmailEditText()
            showLoginWithPassword(activity, email?.text?.toString())
        }

        manualLoginTextView.text = span
        manualLoginTextView.movementMethod = LinkMovementMethod.getInstance()
        manualLoginTextView.highlightColor = Color.TRANSPARENT
        return view
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
        const val LOGIN_MANUALLY_SUBSTRING = "log in manually"
        
        fun showLoginWithPassword(activity: Activity?, username: String?) {
            activity?.let { act ->
                val intent = Intent(act, NewCredentialsActivity::class.java)
                intent.putExtra("EXTRA_IS_LOGIN", true)
                if (!username.isNullOrBlank()) {
                    intent.putExtra(Intent.EXTRA_EMAIL, username)
                    intent.putExtra(NewCredentialsActivity.PREF_HIDE_EMAIL_FIELD, true)
                }
                activity.startActivity(intent)
                act.finish()
            }
        }
    }

}
