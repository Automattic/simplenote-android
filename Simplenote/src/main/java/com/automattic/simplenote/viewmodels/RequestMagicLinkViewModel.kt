package com.automattic.simplenote.viewmodels

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.R
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.repositories.MagicLinkRepository
import com.automattic.simplenote.repositories.MagicLinkResponseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

const val TAG = "RequestMagicLinkViewModel"

@HiltViewModel
class RequestMagicLinkViewModel @Inject constructor(
    private val magicLinkRepository: MagicLinkRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _magicLinkRequestUiState = MutableLiveData<MagicLinkRequestUiState>(MagicLinkRequestUiState.Waiting)
    val magicLinkRequestUiState: LiveData<MagicLinkRequestUiState> get() = _magicLinkRequestUiState

    fun requestLogin(username: String) = viewModelScope.launch(ioDispatcher) {
        _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Loading(
            messageRes = R.string.magic_link_request_login_loading_message)
        )
        try {
            when (val response = magicLinkRepository.requestLogin(username)) {
                is MagicLinkResponseResult.MagicLinkRequestSuccess -> {
                    Log.d(TAG, "Request magic link login success: ${response.code}")
                    _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Success(username = username))
                }
                is MagicLinkResponseResult.MagicLinkError -> {
                    Log.e(TAG, "Request magic link login error: ${response.code}")
                    _magicLinkRequestUiState.postValue(
                        MagicLinkRequestUiState.Error(
                            response.code,
                            if (response.code == 429) R.string.magic_link_error_too_many_requests_enter_password_message else R.string.magic_link_request_error_message
                        )
                    )
                }
                else -> _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Error(null, R.string.magic_link_request_error_message))
            }
        } catch (exception: IOException) {
            _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Error(null, R.string.magic_link_request_error_message))
        }
    }

    fun resetState() {
        _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Waiting)
    }

}

sealed class MagicLinkRequestUiState {
    data class Loading(@StringRes val messageRes: Int): MagicLinkRequestUiState()
    data class Error(val code: Int? = null, @StringRes val messageRes: Int): MagicLinkRequestUiState()
    data class Success(val username: String) : MagicLinkRequestUiState()
    object Waiting : MagicLinkRequestUiState()
}
