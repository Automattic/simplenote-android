package com.automattic.simplenote.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.R
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.repositories.MagicLinkRepository
import com.automattic.simplenote.repositories.MagicLinkResponseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class CompleteMagicLinkViewModel @Inject constructor(
    private val simplenote: Simplenote,
    private val magicLinkRepository: MagicLinkRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
    private val _magicLinkUiState = MutableLiveData<MagicLinkUiState>()
    val magicLinkUiState: LiveData<MagicLinkUiState> get() = _magicLinkUiState

    fun completeLogin(username: String, authCode: String) = viewModelScope.launch(ioDispatcher) {
        _magicLinkUiState.postValue(MagicLinkUiState.Loading(messageRes = R.string.magic_link_complete_login_loading_message))
        try {
            when (val result = magicLinkRepository.completeLogin(username, authCode)) {
                is MagicLinkResponseResult.MagicLinkCompleteSuccess -> {
                    simplenote.loginWithToken(result.username, result.syncToken)
                    _magicLinkUiState.postValue(MagicLinkUiState.Success)
                }
                is MagicLinkResponseResult.MagicLinkError -> {
                    if (result.code == 400) {
                        _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.magic_link_complete_login_error_message))
                    } else {
                        _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.dialog_message_signup_error))
                    }
                }
                else -> _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.dialog_message_signup_error))
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
