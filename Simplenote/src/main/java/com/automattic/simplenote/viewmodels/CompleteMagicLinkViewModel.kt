package com.automattic.simplenote.viewmodels

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.R
import com.automattic.simplenote.authentication.magiclink.MagicLinkAuthError
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
    private val magicLinkRepository: MagicLinkRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
    private val _magicLinkUiState = MutableLiveData<MagicLinkUiState>(MagicLinkUiState.Waiting)
    val magicLinkUiState: LiveData<MagicLinkUiState> get() = _magicLinkUiState

    private var lastKnownUserName: String? = null
    private var lastKnownAuthCode: String? = null

    fun completeLogin(username: String, authCode: String, userInitiated: Boolean = false) = viewModelScope.launch(ioDispatcher) {
        if (!userInitiated && lastKnownUserName == username && lastKnownAuthCode == authCode) {
            Log.d(TAG, "Already checked deeplinked magic link")
            return@launch
        }
        lastKnownUserName = username
        lastKnownAuthCode = authCode
        _magicLinkUiState.postValue(MagicLinkUiState.Loading(messageRes = R.string.magic_link_complete_login_loading_message))
        try {
            when (val result = magicLinkRepository.completeLogin(username, authCode)) {
                is MagicLinkResponseResult.MagicLinkCompleteSuccess -> {
                    _magicLinkUiState.postValue(MagicLinkUiState.Success(result.username, result.syncToken))
                }
                is MagicLinkResponseResult.MagicLinkError -> {
                    _magicLinkUiState.postValue(MagicLinkUiState.Error(result.authError, messageRes = result.errorMessage ?: R.string.magic_link_general_error))
                }
                else -> _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.magic_link_general_error))
            }
        } catch (exception: IOException) {
            _magicLinkUiState.postValue(MagicLinkUiState.Error(messageRes = R.string.magic_link_general_error))
        }
    }

    fun resetState() {
        _magicLinkUiState.postValue(MagicLinkUiState.Waiting)
    }
}

/**
 * Models the Simple UI state for auth, which is just loading, success, and error.
 */
sealed class MagicLinkUiState {
    data class Loading(@StringRes val messageRes: Int): MagicLinkUiState()
    data class Error(val authError: MagicLinkAuthError = MagicLinkAuthError.UNKNOWN_ERROR, @StringRes val messageRes: Int): MagicLinkUiState()
    data class Success(val email: String, val token: String) : MagicLinkUiState()
    object Waiting : MagicLinkUiState()
}
