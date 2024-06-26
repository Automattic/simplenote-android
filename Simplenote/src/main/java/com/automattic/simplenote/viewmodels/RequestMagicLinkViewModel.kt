package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.networking.SimpleHttp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class RequestMagicLinkViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _magicLinkRequestUiState = MutableLiveData<MagicLinkRequestUiState>()
    val magicLinkRequestUiState: LiveData<MagicLinkRequestUiState> get() = _magicLinkRequestUiState

    fun requestLogin(username: String) = viewModelScope.launch(ioDispatcher) {
        _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Loading(message = "Requesting Magic Link"))
        try {
            val requestBody = SimpleHttp.buildRequestBodyFromMap(mapOf(Pair("username", username), Pair("request_source", "android")))
            SimpleHttp.firePostRequest(
                "account/request-login",
                requestBody
            ).use { response ->
                if (response.isSuccessful) {
                    _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Success(username = username))
                } else {
                    _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Error())
                }
            }
        } catch (exception: IOException) {
            _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Error())
        }
    }

}

sealed class MagicLinkRequestUiState {
    data class Loading(val message: String): MagicLinkRequestUiState()
    data class Error(val message: String? = null): MagicLinkRequestUiState()
    data class Success(val username: String) : MagicLinkRequestUiState()
}
