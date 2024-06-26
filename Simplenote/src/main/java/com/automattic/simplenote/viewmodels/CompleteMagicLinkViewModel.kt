package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val simplenote: Simplenote,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
    private val _magicLinkUiState = MutableLiveData<MagicLinkUiState>()
    val magicLinkUiState: LiveData<MagicLinkUiState> get() = _magicLinkUiState

    fun completeLogin(authKey: String, authCode: String) = viewModelScope.launch(ioDispatcher) {
        _magicLinkUiState.postValue(MagicLinkUiState.Loading(message = "Logging In"))
        try {
            val requestBody = SimpleHttp.buildRequestBodyFromMap(mapOf(Pair("auth_key", authKey), Pair("auth_code", authCode)))
            SimpleHttp.firePostRequest(
                "account/complete-login",
                requestBody
            ).use { response ->
                if (response.isSuccessful) {
                    _magicLinkUiState.postValue(MagicLinkUiState.Success)
                    val body = response.body()?.string()
                    val json = JSONObject(body ?: "")
                    val syncToken = json.getString("sync_token")
                    val username = json.getString("username")
                    simplenote.loginWithToken(username, syncToken)
                } else {
                    _magicLinkUiState.value = MagicLinkUiState.Error()
                }
            }
        } catch (exception: IOException) {
            _magicLinkUiState.postValue(MagicLinkUiState.Success)
        }
    }
}

/**
 * Models the Simple UI state for auth, which is just loading, success, and error.
 */
sealed class MagicLinkUiState {
    data class Loading(val message: String): MagicLinkUiState()
    data class Error(val message: String? = null): MagicLinkUiState()
    object Success : MagicLinkUiState()
}
