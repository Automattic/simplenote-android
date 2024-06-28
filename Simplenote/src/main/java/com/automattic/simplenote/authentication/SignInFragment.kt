package com.automattic.simplenote.authentication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.automattic.simplenote.R
import com.automattic.simplenote.utils.NetworkUtils
import com.automattic.simplenote.viewmodels.MagicLinkRequestUiState
import com.automattic.simplenote.viewmodels.RequestMagicLinkViewModel
import com.simperium.android.CredentialsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment: MagicLinkableFragment() {

    var viewModel: RequestMagicLinkViewModel? = null

    private var loginWithPassword: TextView? = null

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        loginWithPassword = view.findViewById(R.id.login_with_password_button)
        loginWithPassword?.setOnClickListener { _ ->
            activity?.let { act ->
                val intent = Intent(act, SimplenoteCredentialsActivity::class.java)
                intent.putExtra("EXTRA_IS_LOGIN", true)
                this.startActivity(intent)
                act.finish()
            }
        }
        return view
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this).get(RequestMagicLinkViewModel::class.java)
        viewModel?.magicLinkRequestUiState?.observe(this.viewLifecycleOwner) { state ->
            when (state) {
                is MagicLinkRequestUiState.Loading -> {
                    showProgressDialog(state.message)
                }
                is MagicLinkRequestUiState.Error -> hideDialogProgress()
                is MagicLinkRequestUiState.Success -> {
                    hideDialogProgress()
                    showConfirmationScreen(state.username, false)
                }
            }
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
