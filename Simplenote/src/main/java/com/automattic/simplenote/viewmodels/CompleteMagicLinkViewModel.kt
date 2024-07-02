package com.automattic.simplenote.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.R
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.networking.SimpleHttp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class CompleteMagicLinkViewModel @Inject constructor(
    private val simplenote: Simplenote,
    private val simpleHttp: SimpleHttp,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
    private val _magicLinkUiState = MutableLiveData<MagicLinkUiState>()
    val magicLinkUiState: LiveData<MagicLinkUiState> get() = _magicLinkUiState

    fun completeLogin(authKey: String, authCode: String) = viewModelScope.launch(ioDispatcher) {
        _magicLinkUiState.postValue(MagicLinkUiState.Loading(messageRes = R.string.magic_link_complete_login_loading_message))
        try {
            simpleHttp.firePostRequest(
                "account/complete-login",
                mapOf(Pair("auth_key", authKey), Pair("auth_code", authCode))
            ).use { response ->
                if (response.isSuccessful) {
                    _magicLinkUiState.postValue(MagicLinkUiState.Success)
                    val body = response.body()?.string()
                    val json = JSONObject(body ?: "")
                    val syncToken = json.getString("sync_token")
                    val username = json.getString("username")
                    simplenote.loginWithToken(username, syncToken)
                } else if (response.code() == 400) {
                    _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.magic_link_complete_login_error_message))
                } else {
                    _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.dialog_message_signup_error))
                }
            }
        } catch (exception: IOException) {
            _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.dialog_message_signup_error))
        }
    }
}

/**
 * Models the Simple UI state for auth, which is just loading, success, and error.
 */
sealed class MagicLinkUiState {
    data class Loading(@StringRes val messageRes: Int): MagicLinkUiState()
    data class Error(@StringRes val messageRes: Int): MagicLinkUiState()
    object Success : MagicLinkUiState()
}
